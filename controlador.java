package Controlador;

import Modelo.Carta;
import Modelo.modelo;
import Vista.InGame;
import Vista.Menu;
import Vista.reglas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Controlador del juego de Truco — versión corregida y mejorada:
 *
 *  FIX 1: Carta del medio ya no lanza la de la izquierda.
 *         Se cuenta cuántos botones visibles hay antes del presionado
 *         para calcular el índice real en la lista Mano.
 *
 *  FIX 2: Puntos de truco solo se suman al GANAR LA MANO (2 de 3 rondas).
 *
 *  FIX 3: Envido solo habilitado ANTES de jugar la primera carta.
 *
 *  FIX 4: Empate resuelto correctamente por jerarquía de cartas (Juego.java).
 *
 *  FIX 5: Turno alternado — en la primera ronda empieza el jugador;
 *         en rondas siguientes empieza quien ganó la anterior.
 *         Si la IA empieza la ronda, juega primero automáticamente.
 *
 *  VISUAL: Cartas jugadas aparecen en la mesa acumuladas, con historial
 *          reposicionado para no tapar el menú de cantos.
 */
public class controlador implements ActionListener {

    public modelo  m1;
    public Menu    v1;
    public InGame  v3;

    // Cartas en mesa de la ronda actual (para el historial visual)
    private ImageIcon iconoCartaJugadorMesa = null;
    private ImageIcon iconoCartaIAMesa      = null;
    private String   textoCartaJugadorMesa  = "";
    private String   textoCartaIAMesa       = "";

    // Número de ronda iniciada automáticamente por la IA
    private int rondaIniciadaPorIA = 0; // cuántas rondas empezó la IA

    public controlador(modelo m1, Menu v1) {
        this.m1 = m1;
        this.v1 = v1;
        this.v3 = new InGame();

        v1.BotonJugar.addActionListener(this);
        v1.BotonReglas.addActionListener(this);
        v1.BotonSalir.addActionListener(e -> System.exit(0));

        registrarListeners();
        v1.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(v1.BotonJugar)) {
            m1.juego.RepartirCartas();
            iniciarJuego();
            return;
        }
        if (ae.getSource().equals(v1.BotonReglas)) {
            reglas v2 = new reglas();
            v2.BotonVolver.addActionListener(ev -> {
                v2.dispose();
                v1.setVisible(true);
            });
            StringBuilder texto = new StringBuilder();
            try {
                texto = m1.LeerReglas();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(controlador.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, "No se pudo cargar el archivo de reglas.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            v2.TextAreaReglas.setText(texto.toString());
            v3.setVisible(false);
            v2.setVisible(true);
            v1.setVisible(false);
            return;
        }

        // Los botones de carta usan lambdas en registrarListeners(); este bloque
        // cubre el caso de que actionPerformed aún los reciba (compatibilidad).
        JButton[] botones = {v3.BotonCarta1, v3.BotonCarta2, v3.BotonCarta3};
        for (int i = 0; i < 3; i++) {
            if (ae.getSource().equals(botones[i])) {
                handleBoton(i);
                return;
            }
        }
    }

    // ── Inicio de juego ───────────────────────────────────────────────────────
    public void iniciarJuego() {
        iconoCartaJugadorMesa = null;
        iconoCartaIAMesa      = null;
        textoCartaJugadorMesa = "";
        textoCartaIAMesa      = "";
        rondaIniciadaPorIA    = 0;
        // FIX BUG 1: resetear estado de turno siempre al iniciar nueva mano
        iaYaJugoEstaRonda = false;
        cartaIAEstaRonda  = null;
        cartasIAOcultas   = 0;
        iaDebeJugarDespuesDeEnvido = false;
        envidoPendienteDeIA        = false;

        mostrarManoJugador();
        mostrarManoIA();
        limpiarCartasMesa();
        actualizarPuntajes();
        habilitarCantos(true);
        ocultarRespuesta();
        v1.setVisible(false);
        v3.setVisible(true);

        // Si la IA es mano en esta mano, sale primero
        if (!m1.juego.isJugadorEsMano()) {
            setMensaje("La IA es mano — sale primero.");
            esperandoIA = true;
            v3.BotonCarta1.setEnabled(false);
            v3.BotonCarta2.setEnabled(false);
            v3.BotonCarta3.setEnabled(false);
            Timer t = new Timer(800, ev -> iaJugaPrimero());
            t.setRepeats(false);
            t.start();
        } else {
            esperandoIA = false;
            setMensaje("Es tu turno — elegí una carta o cantá.");
        }
    }

    // ── Turno del jugador ─────────────────────────────────────────────────────
    /**
     * botonIdx: índice visual del botón presionado (0=izquierda, 1=medio, 2=derecha).
     * Mapeo correcto: contar cuántos botones visibles hay ANTES del presionado.
     */
    private void procesarTurno(int botonIdx) {
        if (m1.juego.isTrucoPendiente() && !m1.juego.isTrucoQuienCanto()) {
            setMensaje("¡Primero respondé al Truco de la IA!");
            return;
        }

        // Si el jugador aceptó envido pero no subió más → se resuelve al tirar carta
        if (m1.juego.isEnvidoAceptado() && m1.juego.getEstadoEnvido() > 0) {
            String resEnvido = m1.juego.resolverEnvidoFinal();
            if (resEnvido != null) {
                setMensaje(resEnvido);
                actualizarPuntajes();
            }
            v3.BtnEnvido.setEnabled(false);
            v3.BtnRealEnvido.setEnabled(false);
            v3.BtnFaltaEnvido.setEnabled(false);
            if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                finalizarJuego(); return;
            }
        }

        habilitarCantos(false);
        ocultarRespuesta();

        JButton[] botones = {v3.BotonCarta1, v3.BotonCarta2, v3.BotonCarta3};
        JLabel[]  labels  = {v3.LabelCarta1, v3.LabelCarta2, v3.LabelCarta3};

        // FIX CARTA DEL MEDIO: contar cuántos botones visibles hay antes del presionado
        int indiceEnMano = 0;
        for (int i = 0; i < botonIdx; i++) {
            if (botones[i].isVisible()) indiceEnMano++;
        }

        // Seleccionar carta usando índice real en la lista Mano
        m1.juego.getJugador1().SeleccionarcartaPorIndice(indiceEnMano);
        Carta cj = m1.juego.getJugador1().getCartaElegida();

        // Una carta fue tirada → el envido ya no puede cantarse
        m1.juego.registrarCartaJugador();

        // Ocultar botón y label del slot presionado
        labels[botonIdx].setVisible(false);
        botones[botonIdx].setVisible(false);

        // Mostrar carta del jugador en mesa central
        ImageIcon icoJ = getIconoEscalado(cj, InGame.CARD_W_PUBLIC, InGame.CARD_H_PUBLIC);
        setCartaLabel(v3.LabelCartaJugador, cj);
        iconoCartaJugadorMesa = icoJ;
        textoCartaJugadorMesa = cj.getNumero() + " " + cj.getPalo();

        // --- IA juega ---
        m1.juego.getIA1().Seleccionarcarta(cj);
        Carta ci = m1.juego.getIA1().getCartaElegida();
        setCartaLabel(v3.LabelCartaIA, ci);
        ocultarUnaCartaIA();

        ImageIcon icoI = getIconoEscalado(ci, InGame.CARD_W_PUBLIC, InGame.CARD_H_PUBLIC);
        iconoCartaIAMesa = icoI;
        textoCartaIAMesa = ci.getNumero() + " " + ci.getPalo();

        // Actualizar panel perceptrones
        v3.actualizarPanelPerceptrones(m1.juego.getIA1());

        // --- Evaluar ronda / mano ---
        char resultado = m1.juego.Decidirganador();
        int rondaIdx   = m1.juego.getRondaActual() - 1; // 0-based

        // Guardar en historial visual
        v3.registrarRondaEnHistorial(rondaIdx,
            icoJ, textoCartaJugadorMesa,
            icoI, textoCartaIAMesa);

        // Limpiar la zona central para la próxima ronda
        // (las cartas quedan en el historial)
        Timer limpiarTimer = new Timer(900, ev -> {
            v3.LabelCartaJugador.setIcon(null);
            v3.LabelCartaJugador.setText("—");
            v3.LabelCartaIA.setIcon(null);
            v3.LabelCartaIA.setText("—");
        });
        limpiarTimer.setRepeats(false);
        limpiarTimer.start();

        switch (resultado) {
            case 's':
                setMensaje("Ganaste esta ronda. ¡Seguimos!");
                actualizarPuntajes();
                habilitarCantos(true);
                // Jugador ganó → le toca salir primero en la siguiente ronda
                break;
            case 'p':
                setMensaje("La IA ganó esta ronda. Seguimos...");
                actualizarPuntajes();
                esperandoIA = true;
                v3.BotonCarta1.setEnabled(false);
                v3.BotonCarta2.setEnabled(false);
                v3.BotonCarta3.setEnabled(false);
                Timer tIA = new Timer(1000, ev -> iaJugaPrimero());
                tIA.setRepeats(false);
                tIA.start();
                break;
            case 'e':
                setMensaje("¡Parda! Empate en la ronda. Seguimos...");
                actualizarPuntajes();
                // En empate, quien era mano sigue siendo mano
                if (!m1.juego.isTurnoJugador()) {
                    esperandoIA = true;
                    v3.BotonCarta1.setEnabled(false);
                    v3.BotonCarta2.setEnabled(false);
                    v3.BotonCarta3.setEnabled(false);
                    Timer tEmp = new Timer(1000, ev -> iaJugaPrimero());
                    tEmp.setRepeats(false);
                    tEmp.start();
                } else {
                    habilitarCantos(true);
                }
                break;
            case 'S': {
                int pts = m1.juego.puntosGanadosPorTruco();
                m1.juego.getJugador1().setPuntaje(m1.juego.getJugador1().getPuntaje() + pts);
                setMensaje("¡Ganaste la mano! (+" + pts + " pto" + (pts > 1 ? "s" : "") + ")");
                actualizarPuntajes();
                verificarFinYNuevaMano();
                return;
            }
            case 'P': {
                int pts = m1.juego.puntosGanadosPorTruco();
                m1.juego.getIA1().setPuntaje(m1.juego.getIA1().getPuntaje() + pts);
                setMensaje("La IA ganó la mano. (+" + pts + " pto" + (pts > 1 ? "s" : "") + " para IA)");
                actualizarPuntajes();
                verificarFinYNuevaMano();
                return;
            }
        }

        if (m1.juego.isManoTerminada()) {
            actualizarPuntajes();
            verificarFinYNuevaMano();
        }
    }

    /**
     * La IA juega su carta primero (cuando ganó la ronda anterior o es mano).
     * Antes de tirar, puede cantar Envido (si rondaActual==0) o Truco.
     */
    private void iaJugaPrimero() {
        m1.juego.getIA1().EvaluarPerceptrones();

        // IA puede iniciar envido solo si todavía no tiró su carta en esta primera ronda
        if (m1.juego.iaPuedeCanarEnvido() && m1.juego.getIA1().getSalidaEnvido() == 1) {
            String respEnvido = m1.juego.iaCantaEnvido();
            if (respEnvido != null) {
                setMensaje(respEnvido);
                actualizarPuntajes();
                v3.actualizarPanelPerceptrones(m1.juego.getIA1());
                // Mostrar botones Quiero/No quiero al jugador para responder al envido
                mostrarRespuestaEnvido();
                esperandoIA = false;
                v3.BotonCarta1.setEnabled(false); // no puede tirar hasta responder
                v3.BotonCarta2.setEnabled(false);
                v3.BotonCarta3.setEnabled(false);
                // Deshabilitar todo: el jugador responde con Quiero/No quiero/Real Envido/Falta Envido
                v3.BtnTruco.setEnabled(false);
                v3.BtnRetruco.setEnabled(false);
                v3.BtnVale4.setEnabled(false);
                v3.BtnEnvido.setEnabled(false);
                // Real Envido y Falta Envido se habilitan: el jugador puede subir directo en vez de Quiero
                v3.BtnRealEnvido.setEnabled(m1.juego.puedeCanarEnvido());
                v3.BtnFaltaEnvido.setEnabled(m1.juego.puedeCanarEnvido());
                iaDebeJugarDespuesDeEnvido = true;
                return;
            }
        }

        // FIX BUG 2: IA puede iniciar truco antes de tirar
        if (m1.juego.getEstadoTruco() == 0 && m1.juego.getIA1().getSalidaTruco() == 1) {
            String respTruco = m1.juego.iaCantaTruco();
            if (respTruco != null) {
                setMensaje(respTruco);
                actualizarPuntajes();
                v3.actualizarPanelPerceptrones(m1.juego.getIA1());
                mostrarRespuesta(); // Quiero/No quiero truco
                esperandoIA = false;
                v3.BotonCarta1.setEnabled(false);
                v3.BotonCarta2.setEnabled(false);
                v3.BotonCarta3.setEnabled(false);
                // Truco: deshabilitar el botón Truco base (ya se cantó)
                v3.BtnTruco.setEnabled(false);
                // Envido: PUEDE cantarse mientras rondaActual==0, incluso con truco pendiente
                boolean puedeEnv = m1.juego.puedeCanarEnvido() && !m1.juego.isEnvidoPendiente();
                int envEstado = m1.juego.getEstadoEnvido();
                v3.BtnEnvido.setEnabled(puedeEnv && envEstado == 0);
                v3.BtnRealEnvido.setEnabled(puedeEnv && envEstado == 1);
                v3.BtnFaltaEnvido.setEnabled(puedeEnv && (envEstado == 1 || envEstado == 2));
                // Retruco habilitado si la IA cantó Truco (estado 1), Vale4 si cantó Retruco (estado 2)
                v3.BtnRetruco.setEnabled(m1.juego.getEstadoTruco() == 1);
                v3.BtnVale4.setEnabled(m1.juego.getEstadoTruco() == 2);
                iaDebeJugarDespuesDeEnvido = true; // reutilizamos: IA aún no tiró carta
                return;
            }
        }

        iaJugarCartaPrimero();
    }

    private boolean iaDebeJugarDespuesDeEnvido = false;

    /** La IA tira su carta (sale primero, sin cantos pendientes). */
    private void iaJugarCartaPrimero() {
        m1.juego.getIA1().EvaluarPerceptrones();
        Carta ci = iaSeleccionarCartaSinRival();
        setCartaLabel(v3.LabelCartaIA, ci);
        ocultarUnaCartaIA();
        iconoCartaIAMesa = getIconoEscalado(ci, InGame.CARD_W_PUBLIC, InGame.CARD_H_PUBLIC);
        textoCartaIAMesa = ci.getNumero() + " " + ci.getPalo();

        // La IA tiró carta → la IA ya no puede cantar envido, pero el jugador sí puede aún
        m1.juego.registrarCartaIA();

        v3.actualizarPanelPerceptrones(m1.juego.getIA1());
        setMensaje("La IA tiró su carta. Ahora jugás vos.");
        habilitarCantos(true);
        esperandoIA = false;
        v3.BotonCarta1.setEnabled(true);
        v3.BotonCarta2.setEnabled(true);
        v3.BotonCarta3.setEnabled(true);

        iaYaJugoEstaRonda = true;
        cartaIAEstaRonda  = ci;
        iaDebeJugarDespuesDeEnvido = false;
    }

    private boolean iaYaJugoEstaRonda = false;
    private Carta   cartaIAEstaRonda  = null;
    private boolean esperandoIA       = false; // bloquea input del jugador mientras la IA va a tirar

    /**
     * Versión del turno del jugador cuando la IA ya jugó primero en esta ronda.
     */
    private void procesarTurnoRespuestaJugador(int botonIdx) {
        habilitarCantos(false);
        ocultarRespuesta();

        JButton[] botones = {v3.BotonCarta1, v3.BotonCarta2, v3.BotonCarta3};
        JLabel[]  labels  = {v3.LabelCarta1, v3.LabelCarta2, v3.LabelCarta3};

        int indiceEnMano = 0;
        for (int i = 0; i < botonIdx; i++) {
            if (botones[i].isVisible()) indiceEnMano++;
        }

        m1.juego.getJugador1().SeleccionarcartaPorIndice(indiceEnMano);
        Carta cj = m1.juego.getJugador1().getCartaElegida();

        labels[botonIdx].setVisible(false);
        botones[botonIdx].setVisible(false);

        // Ya se guardó la carta de la IA — solo mostramos la del jugador
        setCartaLabel(v3.LabelCartaJugador, cj);
        ImageIcon icoJ = getIconoEscalado(cj, InGame.CARD_W_PUBLIC, InGame.CARD_H_PUBLIC);
        iconoCartaJugadorMesa = icoJ;
        textoCartaJugadorMesa = cj.getNumero() + " " + cj.getPalo();

        // Establecer carta elegida manualmente en el jugador para Decidirganador
        // (ya está seteada por SeleccionarcartaPorIndice)
        // La carta de la IA ya fue seteada en iaJugaPrimero

        v3.actualizarPanelPerceptrones(m1.juego.getIA1());

        char resultado = m1.juego.Decidirganador();
        int rondaIdx   = m1.juego.getRondaActual() - 1;

        v3.registrarRondaEnHistorial(rondaIdx,
            icoJ, textoCartaJugadorMesa,
            iconoCartaIAMesa, textoCartaIAMesa);

        Timer limpiarTimer = new Timer(900, ev -> {
            v3.LabelCartaJugador.setIcon(null);
            v3.LabelCartaJugador.setText("—");
            v3.LabelCartaIA.setIcon(null);
            v3.LabelCartaIA.setText("—");
        });
        limpiarTimer.setRepeats(false);
        limpiarTimer.start();

        iaYaJugoEstaRonda = false;
        cartaIAEstaRonda  = null;

        switch (resultado) {
            case 's':
                setMensaje("Ganaste esta ronda. ¡Seguimos!");
                actualizarPuntajes();
                habilitarCantos(true);
                break;
            case 'p':
                setMensaje("La IA ganó esta ronda. Seguimos...");
                actualizarPuntajes();
                esperandoIA = true;
                v3.BotonCarta1.setEnabled(false);
                v3.BotonCarta2.setEnabled(false);
                v3.BotonCarta3.setEnabled(false);
                Timer tIA = new Timer(1000, ev -> iaJugaPrimero());
                tIA.setRepeats(false);
                tIA.start();
                break;
            case 'e':
                setMensaje("¡Parda! Empate en la ronda. Seguimos...");
                actualizarPuntajes();
                // En empate, quien era mano sigue saliendo primero
                if (!m1.juego.isTurnoJugador()) {
                    esperandoIA = true;
                    v3.BotonCarta1.setEnabled(false);
                    v3.BotonCarta2.setEnabled(false);
                    v3.BotonCarta3.setEnabled(false);
                    Timer tEmp = new Timer(1000, ev -> iaJugaPrimero());
                    tEmp.setRepeats(false);
                    tEmp.start();
                } else {
                    habilitarCantos(true);
                }
                break;
            case 'S': {
                int pts = m1.juego.puntosGanadosPorTruco();
                m1.juego.getJugador1().setPuntaje(m1.juego.getJugador1().getPuntaje() + pts);
                setMensaje("¡Ganaste la mano! (+" + pts + " pto" + (pts > 1 ? "s" : "") + ")");
                actualizarPuntajes();
                verificarFinYNuevaMano();
                return;
            }
            case 'P': {
                int pts = m1.juego.puntosGanadosPorTruco();
                m1.juego.getIA1().setPuntaje(m1.juego.getIA1().getPuntaje() + pts);
                setMensaje("La IA ganó la mano. (+" + pts + " pto" + (pts > 1 ? "s" : "") + " para IA)");
                actualizarPuntajes();
                verificarFinYNuevaMano();
                return;
            }
        }

        if (m1.juego.isManoTerminada()) {
            actualizarPuntajes();
            verificarFinYNuevaMano();
        }
    }

    /** Override de procesarTurno para despachar según estado */
    private void handleBoton(int botonIdx) {
        if (esperandoIA) return; // IA está por tirar, ignorar clicks
        if (m1.juego.isTrucoPendiente() && !m1.juego.isTrucoQuienCanto()) {
            setMensaje("¡Primero respondé al Truco de la IA!");
            return;
        }
        if (iaYaJugoEstaRonda) {
            procesarTurnoRespuestaJugador(botonIdx);
        } else {
            procesarTurno(botonIdx);
        }
    }

    /** La IA selecciona carta sin conocer la carta rival (sale primero). */
    private Carta iaSeleccionarCartaSinRival() {
        m1.juego.getIA1().EvaluarPerceptrones();
        java.util.ArrayList<Carta> manoIA = m1.juego.getIA1().getMano();
        if (manoIA.isEmpty()) return m1.juego.getIA1().getCartaElegida();

        Carta elegida;
        if (m1.juego.getIA1().getSalidaDecision() == 1) {
            // Agresivo: carta más fuerte
            elegida = manoIA.get(0);
            for (Carta c : manoIA) {
                if (c.getValor() > elegida.getValor()) elegida = c;
            }
        } else {
            // Conservador: carta más baja
            elegida = manoIA.get(0);
            for (Carta c : manoIA) {
                if (c.getValor() < elegida.getValor()) elegida = c;
            }
        }
        manoIA.remove(elegida);
        m1.juego.getIA1().setCartaElegida(elegida);
        return elegida;
    }

    private void verificarFinYNuevaMano() {
        if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
            finalizarJuego();
            return;
        }
        Timer t = new Timer(1600, ev -> {
            m1.juego.alternarMano();
            m1.juego.RepartirCartas();
            iaYaJugoEstaRonda = false;
            cartaIAEstaRonda  = null;
            esperandoIA       = false;
            iniciarJuego();
        });
        t.setRepeats(false);
        t.start();
    }

    // ── Cantar Envido ─────────────────────────────────────────────────────────
    private void cantarEnvido(int nivel) {
        if (!m1.juego.puedeCanarEnvido()) {
            setMensaje("El envido solo se puede cantar antes de tirar la primera carta.");
            return;
        }

        String resp;

        // Si la IA cantó envido y el jugador sube directamente (Real Envido / Falta Envido)
        if (envidoPendienteDeIA && nivel > m1.juego.getEstadoEnvido()) {
            envidoPendienteDeIA = false;
            ocultarRespuesta();
            resp = m1.juego.jugadorSubeEnvidoDirecto(nivel);
            if (resp == null) resp = m1.juego.jugadorCantaEnvido(nivel);
        } else {
            // Si el envido fue iniciado por la IA y el jugador acepta sin subir → el controlador
            // llama esto solo si el jugador hizo click en un botón de igual nivel (no debería pasar)
            if (envidoPendienteDeIA) {
                envidoPendienteDeIA = false;
                ocultarRespuesta();
            }
            resp = m1.juego.jugadorCantaEnvido(nivel);
        }
        setMensaje(resp);
        actualizarPuntajes();
        v3.actualizarPanelPerceptrones(m1.juego.getIA1());

        // Si la IA no quiso → cerrar envido, continuar
        if (resp != null && (resp.contains("No quiero") || resp.contains("ganás"))) {
            // envido resuelto — deshabilitar botones de envido
            v3.BtnEnvido.setEnabled(false);
            v3.BtnRealEnvido.setEnabled(false);
            v3.BtnFaltaEnvido.setEnabled(false);
            if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                finalizarJuego();
            } else if (iaDebeJugarDespuesDeEnvido) {
                // IA aún tiene que tirar carta
                iaDebeJugarDespuesDeEnvido = false;
                esperandoIA = true;
                v3.BotonCarta1.setEnabled(false);
                v3.BotonCarta2.setEnabled(false);
                v3.BotonCarta3.setEnabled(false);
                Timer t = new Timer(800, ev -> iaJugarCartaPrimero());
                t.setRepeats(false);
                t.start();
            }
            return;
        }

        // La IA aceptó el envido del jugador → el envido se resolvió
        if (resp != null && (resp.contains("Ganaste") || resp.contains("gana el"))) {
            v3.BtnEnvido.setEnabled(false);
            v3.BtnRealEnvido.setEnabled(false);
            v3.BtnFaltaEnvido.setEnabled(false);
            if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                finalizarJuego();
            }
            return;
        }

        // Si llegamos acá, la IA subió el canto → mostrar botones Quiero/No quiero
        // y habilitar que el jugador suba más
        actualizarBotonesEnvido();
        if (m1.juego.isEnvidoPendiente()) {
            mostrarRespuesta();
            envidoPendienteDeIA = true;
        }
        if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
            finalizarJuego();
        }
    }

    // ── Cantar Truco ──────────────────────────────────────────────────────────
    private void cantarTruco(int nivel) {
        // Si hay un truco pendiente de la IA, el jugador puede subir directamente (sin Quiero)
        String resp;
        if (m1.juego.isTrucoPendiente() && !m1.juego.isTrucoQuienCanto()) {
            resp = m1.juego.jugadorSubeTrucoDirecto(nivel);
            if (resp == null) {
                // No pudo subir directamente, intentar como canto normal
                resp = m1.juego.jugadorCantaTruco(nivel);
            }
        } else {
            resp = m1.juego.jugadorCantaTruco(nivel);
        }
        setMensaje(resp);
        actualizarPuntajes();
        v3.actualizarPanelPerceptrones(m1.juego.getIA1());

        if (resp != null && resp.contains("No quiero")) {
            actualizarPuntajes();
            if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                finalizarJuego();
            } else {
                Timer t = new Timer(1600, ev -> {
                    m1.juego.alternarMano();
                    m1.juego.RepartirCartas();
                    iaYaJugoEstaRonda = false;
                    cartaIAEstaRonda  = null;
                    esperandoIA       = false;
                    iniciarJuego();
                });
                t.setRepeats(false);
                t.start();
            }
            return;
        }

        if (m1.juego.isTrucoPendiente() && !m1.juego.isTrucoQuienCanto()) {
            mostrarRespuesta();
        } else {
            ocultarRespuesta();
        }

        actualizarBotonesTruco();

        if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
            finalizarJuego();
        }
    }

    // ── Responder al canto de la IA (Truco o Envido) ─────────────────────────
    private void responderTruco(boolean acepta) {
        // Si el envido fue cantado por la IA, responder al envido
        if (envidoPendienteDeIA) {
            if (!acepta) {
                // No quiere: resolver directamente
                envidoPendienteDeIA = false;
                String resp = m1.juego.jugadorResponderEnvido(false);
                if (resp != null) { setMensaje(resp); actualizarPuntajes(); }
                ocultarRespuesta();
                v3.BtnEnvido.setEnabled(false);
                v3.BtnRealEnvido.setEnabled(false);
                v3.BtnFaltaEnvido.setEnabled(false);
                v3.actualizarPanelPerceptrones(m1.juego.getIA1());
                if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                    finalizarJuego(); return;
                }
                if (iaDebeJugarDespuesDeEnvido) {
                    iaDebeJugarDespuesDeEnvido = false;
                    esperandoIA = true;
                    v3.BotonCarta1.setEnabled(false);
                    v3.BotonCarta2.setEnabled(false);
                    v3.BotonCarta3.setEnabled(false);
                    Timer t = new Timer(800, ev -> iaJugarCartaPrimero());
                    t.setRepeats(false); t.start();
                } else {
                    v3.BotonCarta1.setEnabled(true);
                    v3.BotonCarta2.setEnabled(true);
                    v3.BotonCarta3.setEnabled(true);
                }
            } else {
                // Quiere: acepta el envido de la IA — puede subir aún
                envidoPendienteDeIA = false;
                String resp = m1.juego.jugadorResponderEnvido(true);
                if (resp != null) { setMensaje(resp); actualizarPuntajes(); }
                ocultarRespuesta();
                v3.actualizarPanelPerceptrones(m1.juego.getIA1());
                // Ahora el envido está aceptado pero NO resuelto aún
                // Habilitar botones para subir (Real Envido, Falta Envido)
                actualizarBotonesEnvido();
                // El jugador también puede simplemente tirar carta (lo cual cierra el envido)
                v3.BotonCarta1.setEnabled(true);
                v3.BotonCarta2.setEnabled(true);
                v3.BotonCarta3.setEnabled(true);
                if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                    finalizarJuego();
                }
            }
            return;
        }

        // Respuesta a Truco cantado por la IA
        String resp = m1.juego.jugadorResponderTruco(acepta);
        if (resp != null) {
            setMensaje(resp);
            actualizarPuntajes();
        }
        ocultarRespuesta();
        actualizarBotonesTruco();

        if (!acepta) {
            actualizarPuntajes();
            if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
                finalizarJuego();
            } else {
                Timer t = new Timer(1600, ev -> {
                    m1.juego.alternarMano();
                    m1.juego.RepartirCartas();
                    iaYaJugoEstaRonda = false;
                    cartaIAEstaRonda  = null;
                    esperandoIA       = false;
                    iniciarJuego();
                });
                t.setRepeats(false);
                t.start();
            }
            return;
        }

        if (m1.juego.getJugador1().getPuntaje() >= 30 || m1.juego.getIA1().getPuntaje() >= 30) {
            finalizarJuego();
            return;
        }

        // Si la IA cantó truco antes de tirar, ahora debe tirar su carta
        if (iaDebeJugarDespuesDeEnvido) {
            iaDebeJugarDespuesDeEnvido = false;
            esperandoIA = true;
            v3.BotonCarta1.setEnabled(false);
            v3.BotonCarta2.setEnabled(false);
            v3.BotonCarta3.setEnabled(false);
            Timer t = new Timer(700, ev -> iaJugarCartaPrimero());
            t.setRepeats(false);
            t.start();
        } else {
            // La IA ya había tirado carta antes de cantar truco → el jugador puede responder
            habilitarCantos(false);
            v3.BotonCarta1.setEnabled(true);
            v3.BotonCarta2.setEnabled(true);
            v3.BotonCarta3.setEnabled(true);
        }
    }

    private void mostrarManoJugador() {
        java.util.ArrayList<Carta> mano = m1.juego.getJugador1().getMano();
        JLabel[]  labels  = {v3.LabelCarta1, v3.LabelCarta2, v3.LabelCarta3};
        JButton[] botones = {v3.BotonCarta1, v3.BotonCarta2, v3.BotonCarta3};
        for (int i = 0; i < 3; i++) {
            if (i < mano.size()) {
                setCartaLabel(labels[i], mano.get(i));
                labels[i].setVisible(true);
                botones[i].setVisible(true);
                botones[i].setEnabled(true); // siempre resetear enabled al mostrar
            } else {
                labels[i].setVisible(false);
                botones[i].setVisible(false);
            }
        }
    }

    private void mostrarManoIA() {
        v3.LabelCartaIA1.setVisible(true);
        v3.LabelCartaIA2.setVisible(true);
        v3.LabelCartaIA3.setVisible(true);
    }

    private int cartasIAOcultas = 0;

    private void ocultarUnaCartaIA() {
        cartasIAOcultas++;
        if (cartasIAOcultas >= 1) v3.LabelCartaIA1.setVisible(false);
        if (cartasIAOcultas >= 2) v3.LabelCartaIA2.setVisible(false);
        if (cartasIAOcultas >= 3) v3.LabelCartaIA3.setVisible(false);
    }

    private void limpiarCartasMesa() {
        v3.LabelCartaJugador.setIcon(null);
        v3.LabelCartaJugador.setText("—");
        v3.LabelCartaIA.setIcon(null);
        v3.LabelCartaIA.setText("—");
        v3.limpiarRondas();
        cartasIAOcultas = 0;
    }

    private ImageIcon getIconoEscalado(Carta carta, int w, int h) {
        ImageIcon ico = carta.getImagen();
        if (ico == null) return null;
        java.awt.Image img = ico.getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void setCartaLabel(JLabel lbl, Carta carta) {
        ImageIcon ico = carta.getImagen();
        if (ico != null) {
            java.awt.Image img = ico.getImage().getScaledInstance(
                    InGame.CARD_W_PUBLIC, InGame.CARD_H_PUBLIC,
                    java.awt.Image.SCALE_SMOOTH);
            lbl.setIcon(new ImageIcon(img));
            lbl.setText("");
        } else {
            lbl.setIcon(null);
            lbl.setText(carta.getNumero() + " " + carta.getPalo());
        }
    }

    private void actualizarPuntajes() {
        v3.PuntajeJugador.setText("Vos: " + m1.juego.getJugador1().getPuntaje());
        v3.PuntajeIA.setText("IA: "  + m1.juego.getIA1().getPuntaje());
    }

    private void setMensaje(String msg) {
        if (msg != null && msg.contains("\n")) {
            msg = "<html>" + msg.replace("\n", "<br>") + "</html>";
        }
        v3.LabelMensaje.setText(msg);
    }

    private void habilitarCantos(boolean hab) {
        actualizarBotonesEnvido();
        int estado   = m1.juego.getEstadoTruco();
        boolean pend = m1.juego.isTrucoPendiente();
        boolean iaCanto = pend && !m1.juego.isTrucoQuienCanto(); // la IA cantó y espera respuesta

        // Truco: solo si nadie cantó todavía
        v3.BtnTruco.setEnabled(hab && estado == 0 && !pend);
        // Retruco: si el estado es 1 (truco cantado) y:
        //   a) fue aceptado (flujo normal), o
        //   b) la IA acaba de cantar truco y el jugador puede subir directamente
        v3.BtnRetruco.setEnabled(hab && estado == 1 && (m1.juego.isTrucoAceptado() || iaCanto));
        // Vale4: si el estado es 2 (retruco cantado) y:
        //   a) fue aceptado, o
        //   b) la IA acaba de cantar retruco y el jugador puede subir directamente
        v3.BtnVale4.setEnabled(hab && estado == 2 && (m1.juego.isTrucoAceptado() || iaCanto));
    }

    private void actualizarBotonesEnvido() {
        // Puede cantar envido si: rondaActual==0 y envido no fue resuelto
        boolean envDisp = m1.juego.puedeCanarEnvido() && !m1.juego.isEnvidoPendiente();
        int estadoEnv = m1.juego.getEstadoEnvido();
        // Envido: solo si no se cantó nada de envido aún
        v3.BtnEnvido.setEnabled(envDisp && estadoEnv == 0);
        // Real Envido: si envido (nivel 1) fue aceptado y el jugador puede subir
        v3.BtnRealEnvido.setEnabled(envDisp && estadoEnv == 1);
        // Falta Envido: si envido o realEnvido fue aceptado
        v3.BtnFaltaEnvido.setEnabled(envDisp && (estadoEnv == 1 || estadoEnv == 2));
    }

    private void actualizarBotonesTruco() {
        int estado   = m1.juego.getEstadoTruco();
        boolean pend = m1.juego.isTrucoPendiente();
        boolean iaCanto = pend && !m1.juego.isTrucoQuienCanto();

        v3.BtnTruco.setEnabled(estado == 0 && !pend);
        v3.BtnRetruco.setEnabled(estado == 1 && (m1.juego.isTrucoAceptado() || iaCanto));
        v3.BtnVale4.setEnabled(estado == 2 && (m1.juego.isTrucoAceptado() || iaCanto));
    }

    private void mostrarRespuesta() {
        v3.BtnQuiero.setVisible(true);
        v3.BtnNoQuiero.setVisible(true);
    }

    private void mostrarRespuestaEnvido() {
        // Reutilizamos los botones Quiero/NoQuiero pero con contexto de envido
        v3.BtnQuiero.setVisible(true);
        v3.BtnNoQuiero.setVisible(true);
        envidoPendienteDeIA = true;
    }

    private boolean envidoPendienteDeIA = false;

    private void ocultarRespuesta() {
        v3.BtnQuiero.setVisible(false);
        v3.BtnNoQuiero.setVisible(false);
    }

    public void finalizarJuego() {
        String ganador = m1.juego.getJugador1().getPuntaje() >= 30
                ? "¡Ganaste la partida!" : "¡La IA ganó la partida!";
        JOptionPane.showMessageDialog(v3, ganador, "Fin del juego", JOptionPane.INFORMATION_MESSAGE);
        m1.juego = new Modelo.Juego();
        iaYaJugoEstaRonda = false;
        cartaIAEstaRonda  = null;
        cartasIAOcultas   = 0;
        esperandoIA       = false;
        iaDebeJugarDespuesDeEnvido = false;
        envidoPendienteDeIA        = false;
        v3.dispose();
        v3 = new InGame();
        registrarListeners();
        v1.setVisible(true);
    }

    private void registrarListeners() {
        v3.BotonCarta1.addActionListener(e -> handleBoton(0));
        v3.BotonCarta2.addActionListener(e -> handleBoton(1));
        v3.BotonCarta3.addActionListener(e -> handleBoton(2));
        v3.BtnEnvido.addActionListener(e -> cantarEnvido(1));
        v3.BtnRealEnvido.addActionListener(e -> cantarEnvido(2));
        v3.BtnFaltaEnvido.addActionListener(e -> cantarEnvido(3));
        v3.BtnTruco.addActionListener(e -> cantarTruco(1));
        v3.BtnRetruco.addActionListener(e -> cantarTruco(2));
        v3.BtnVale4.addActionListener(e -> cantarTruco(3));
        v3.BtnQuiero.addActionListener(e -> responderTruco(true));
        v3.BtnNoQuiero.addActionListener(e -> responderTruco(false));
    }
}
