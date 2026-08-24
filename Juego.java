package Modelo;

import java.util.Stack;
import java.util.ArrayList;

/**
 * Modelo principal del juego de Truco.
 * Reglas correctas:
 *  - Empate en ronda: en la siguiente ronda gana quien tenga la carta más fuerte.
 *  - Si empatan TODAS las rondas, gana el jugador que es "mano".
 *  - Turno alternado: una yo, una la IA (en cada ronda empieza quien ganó la anterior;
 *    en la primera ronda, el jugador es mano y sale primero).
 *  - Envido solo se puede cantar ANTES de jugar la primera carta (rondaActual == 0).
 *  - Truco no querido termina la mano automáticamente.
 */
public class Juego {

    private Stack<Carta> Mazo;
    private Jugador Jugador1;
    private IA IA1;

    // Estado Truco: 0=no cantado, 1=truco, 2=retruco, 3=vale4
    private int estadoTruco       = 0;
    private int puntosTruco       = 0;
    private boolean trucoAceptado  = false;
    private boolean trucoPendiente = false;
    private boolean trucoQuienCanto = true; // true=jugador cantó, false=IA cantó

    // Estado Envido: 0=no cantado, 1=envido, 2=realEnvido, 3=faltaEnvido, -1=resuelto
    private int estadoEnvido      = 0;
    private boolean envidoAceptado  = false;
    private boolean envidoPendiente = false;

    // Rondas jugadas en la mano actual
    // 'j'=jugador ganó, 'i'=IA ganó, 'e'=empate
    private char[] resultadoRondas = new char[3];
    private int rondaActual = 0;

    // Rastrea si cada uno ya tiró carta en la primera ronda
    // El envido solo puede cantarlo quien todavía NO tiró carta (y solo en ronda 1)
    private boolean jugadorTiroCarta = false;
    private boolean iaTiroCarta      = false;

    // Turno: true = le toca al jugador, false = IA sale primero en esta ronda
    // En la primera ronda siempre empieza el jugador (es mano)
    private boolean turnoJugador = true;

    public Juego() {
        Jugador1 = new Jugador();
        IA1      = new IA();
        this.Mazo = new Stack<>();
        Mazo mazotemporal = new Mazo();
        mazotemporal.mezclar();
        this.Mazo = mazotemporal.getCartaMixed();
    }

    public void RepartirCartas() {
        if (Mazo.size() < 6) {
            Mazo mazotemporal = new Mazo();
            mazotemporal.mezclar();
            this.Mazo = mazotemporal.getCartaMixed();
        }
        Jugador1.getMano().clear();
        IA1.getMano().clear();

        for (int i = 0; i < 3; i++) {
            if (!Mazo.isEmpty()) Jugador1.setMano(Mazo.pop());
            if (!Mazo.isEmpty()) IA1.setMano(Mazo.pop());
        }
        resetEstadoMano();
    }

    // Alterna quién es mano al inicio de cada nueva mano
    private boolean jugadorEsMano = true;

    private void resetEstadoMano() {
        estadoTruco     = 0;
        puntosTruco     = 0;
        trucoAceptado   = false;
        trucoPendiente  = false;
        estadoEnvido    = 0;
        envidoAceptado  = false;
        envidoPendiente = false;
        resultadoRondas = new char[3];
        rondaActual     = 0;
        jugadorTiroCarta = false;
        iaTiroCarta      = false;
        turnoJugador    = jugadorEsMano; // quien es mano sale primero
    }

    /** Alterna quién es mano para la próxima mano */
    public void alternarMano() {
        jugadorEsMano = !jugadorEsMano;
    }

    public boolean isJugadorEsMano() { return jugadorEsMano; }

    public void IniciarJuego() {
        RepartirCartas();
    }

    /**
     * Registra el resultado de la ronda actual y determina si la MANO terminó.
     * Devuelve:
     *   's' = jugador ganó la ronda (mano sigue)
     *   'p' = IA ganó la ronda (mano sigue)
     *   'e' = empate de ronda (mano sigue)
     *   'S' = jugador ganó la MANO
     *   'P' = IA ganó la MANO
     *   'E' = empate total (gana el mano = jugador)
     */
    public char Decidirganador() {
        int vj = Jugador1.getCartaElegida().getValor();
        int vi = IA1.getCartaElegida().getValor();

        char resRonda;
        if (vj > vi)      resRonda = 'j';
        else if (vj < vi) resRonda = 'i';
        else              resRonda = 'e';

        if (rondaActual < 3) {
            resultadoRondas[rondaActual] = resRonda;
            rondaActual++;
        }

        // Actualizar quién gana ronda para el siguiente turno
        if (resRonda == 'j') {
            turnoJugador = true;
            IA1.setGanoRonda(false);
        } else if (resRonda == 'i') {
            turnoJugador = false;
            IA1.setGanoRonda(true);
        }
        // En empate, quien era mano sigue siendo mano (no cambia turno)

        // Contar rondas ganadas
        int rondasJ = 0, rondasI = 0, rondasE = 0;
        for (int i = 0; i < rondaActual; i++) {
            if (resultadoRondas[i] == 'j') rondasJ++;
            else if (resultadoRondas[i] == 'i') rondasI++;
            else rondasE++;
        }

        // La mano termina cuando alguien ganó 2 rondas, se jugaron 3 rondas,
        // o cuando alguien ya ganó 1 y hubo empate (ganó la primera, empata la segunda → gana la mano)
        boolean alguienGano1YEmpate = (rondaActual >= 2)
                && ((rondasJ == 1 && rondasE >= 1 && rondasI == 0)
                ||  (rondasI == 1 && rondasE >= 1 && rondasJ == 0));
        boolean manoTerminada = (rondasJ >= 2 || rondasI >= 2 || rondaActual >= 3 || alguienGano1YEmpate);

        if (!manoTerminada) {
            if (resRonda == 'j') return 's';
            if (resRonda == 'i') return 'p';
            return 'e';
        }

        // --- Resolución con reglas de empate correctas ---
        // Caso: empate en primera ronda, jugador gana segunda -> jugador gana (1 ronda ganada)
        // Caso: empate en primera ronda, IA gana segunda -> IA gana
        // Caso: jugador gana 1ra, empata 2da -> jugador gana (ya ganó la primera)
        // Caso: empate 1ra y 2da -> gana el mano (jugador)
        // Caso: empate todas -> gana el mano (jugador)

        if (rondasJ > rondasI) return 'S';
        if (rondasI > rondasJ) return 'P';

        // Igualdad de rondas ganadas: verificar reglas de empate
        // Si alguien ganó la primera ronda y hubo empates posteriores, gana quien ganó la primera
        if (rondaActual >= 2) {
            char primera = resultadoRondas[0];
            if (primera == 'j') return 'S';
            if (primera == 'i') return 'P';
        }

        // Empate total o situación sin ganador claro -> gana el mano (jugador)
        return 'S';
    }

    public boolean isManoTerminada() {
        int rondasJ = 0, rondasI = 0, rondasE = 0;
        for (int i = 0; i < rondaActual; i++) {
            if (resultadoRondas[i] == 'j') rondasJ++;
            else if (resultadoRondas[i] == 'i') rondasI++;
            else rondasE++;
        }
        boolean alguienGano1YEmpate = (rondaActual >= 2)
                && ((rondasJ == 1 && rondasE >= 1 && rondasI == 0)
                ||  (rondasI == 1 && rondasE >= 1 && rondasJ == 0));
        return (rondasJ >= 2 || rondasI >= 2 || rondaActual >= 3 || alguienGano1YEmpate);
    }

    /** True si en esta ronda le toca salir primero al jugador */
    public boolean isTurnoJugador() { return turnoJugador; }

    public int getRondaActual() { return rondaActual; }
    public char[] getResultadoRondas() { return resultadoRondas; }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRUCO
    // ─────────────────────────────────────────────────────────────────────────

    public String jugadorCantaTruco(int nivel) {
        if (trucoAceptado)      return "El truco ya fue aceptado, jugá una carta.";
        if (nivel <= estadoTruco && estadoTruco > 0)
                                return "No podés cantarlo de nuevo.";
        if (nivel == 2 && estadoTruco < 1) return "Primero tenés que cantar Truco.";
        if (nivel == 3 && estadoTruco < 2) return "Primero tenés que cantar Retruco.";

        estadoTruco     = nivel;
        trucoPendiente  = true;
        trucoQuienCanto = true;
        puntosTruco     = nivel == 1 ? 2 : nivel == 2 ? 3 : 4;

        return iaResponderTruco();
    }

    private String iaResponderTruco() {
        IA1.EvaluarPerceptrones();
        boolean acepta = (IA1.getSalidaTruco() == 1);

        if (acepta) {
            // Si puede subir, la IA sube DIRECTAMENTE (sin decir "Quiero" primero)
            if (estadoTruco < 3) {
                int nuevoNivel = estadoTruco + 1;
                estadoTruco     = nuevoNivel;
                trucoAceptado   = false;
                trucoPendiente  = true;
                trucoQuienCanto = false;
                puntosTruco     = nuevoNivel == 2 ? 3 : 4;
                return "IA: ¡" + nombreTruco(nuevoNivel) + "! (" + puntosTruco + " pts) — ¿Querés?";
            } else {
                // Ya está en Vale4, solo puede aceptar
                trucoAceptado  = true;
                trucoPendiente = false;
                return "IA: ¡Quiero! — " + nombreTruco(estadoTruco) + " (" + puntosTruco + " pts en juego).";
            }
        } else {
            // IA no quiere: jugador cobra puntos ANTERIORES al canto actual
            int ptsCobrar = estadoTruco == 1 ? 1 : estadoTruco == 2 ? 2 : 3;
            Jugador1.setPuntaje(Jugador1.getPuntaje() + ptsCobrar);
            trucoPendiente = false;
            estadoTruco    = 0;
            return "IA: No quiero. Vos ganás " + ptsCobrar + " pto(s).";
        }
    }

    public String jugadorResponderTruco(boolean acepta) {
        if (!trucoPendiente || trucoQuienCanto) return null;
        if (acepta) {
            trucoAceptado  = true;
            trucoPendiente = false;
            return "Aceptaste " + nombreTruco(estadoTruco) + " — " + puntosTruco + " pts en juego.";
        } else {
            int ptsCobrar = estadoTruco == 1 ? 1 : estadoTruco == 2 ? 2 : 3;
            IA1.setPuntaje(IA1.getPuntaje() + ptsCobrar);
            trucoPendiente = false;
            estadoTruco    = 0;
            return "No quisiste. IA gana " + ptsCobrar + " pto(s).";
        }
    }

    /**
     * El jugador sube el truco directamente (sin decir "Quiero" primero).
     * Válido cuando hay un truco pendiente cantado por la IA.
     * Ej: IA canta Truco → jugador puede responder con Retruco directamente.
     * Ej: IA canta Retruco → jugador puede responder con Vale4 directamente.
     */
    public String jugadorSubeTrucoDirecto(int nuevoNivel) {
        if (!trucoPendiente || trucoQuienCanto) return null; // no hay canto pendiente de la IA
        if (nuevoNivel <= estadoTruco) return null;
        if (nuevoNivel == 2 && estadoTruco != 1) return null;
        if (nuevoNivel == 3 && estadoTruco != 2) return null;

        // El jugador acepta implícitamente el canto anterior y sube
        estadoTruco     = nuevoNivel;
        trucoPendiente  = true;
        trucoQuienCanto = true; // ahora cantó el jugador, le toca responder a la IA
        puntosTruco     = nuevoNivel == 2 ? 3 : 4;

        // La IA responde al nuevo canto
        return iaResponderTruco();
    }

    /**
     * La IA sube el truco directamente (sin decir "Quiero" primero).
     * Válido cuando hay un truco pendiente cantado por el jugador.
     * Ej: jugador canta Truco → IA puede responder con Retruco directamente.
     * Ej: jugador canta Retruco → IA puede responder con Vale4 directamente.
     * Retorna el mensaje para mostrar al jugador, o null si no aplica.
     */
    public String iaSubeTrucoDirecto() {
        if (!trucoPendiente || !trucoQuienCanto) return null; // no hay canto pendiente del jugador
        if (estadoTruco >= 3) return null; // ya está en vale4, no puede subir

        int nuevoNivel = estadoTruco + 1;
        // Acepta implícitamente y sube
        estadoTruco     = nuevoNivel;
        trucoPendiente  = true;
        trucoQuienCanto = false; // ahora cantó la IA, le toca responder al jugador
        puntosTruco     = nuevoNivel == 2 ? 3 : 4;

        return "IA sube directo: ¡" + nombreTruco(nuevoNivel) + "! (" + puntosTruco + " pts) — ¿Querés?";
    }

    public String jugadorSubeTruco(int nuevoNivel) {
        return jugadorCantaTruco(nuevoNivel);
    }

    public int puntosGanadosPorTruco() {
        if (!trucoAceptado) return 1;
        return puntosTruco;
    }

    private String nombreTruco(int n) {
        switch (n) {
            case 1: return "Truco";
            case 2: return "Retruco";
            case 3: return "Vale 4";
            default: return "?";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENVIDO — solo se puede cantar en la primera ronda, antes de tirar propia carta
    // ─────────────────────────────────────────────────────────────────────────

    /** El jugador humano acaba de tirar su carta. */
    public void registrarCartaJugador() {
        jugadorTiroCarta = true;
    }

    /** La IA acaba de tirar su carta. */
    public void registrarCartaIA() {
        iaTiroCarta = true;
    }

    /**
     * True si el JUGADOR puede cantar envido:
     * - Solo en la primera ronda (rondaActual == 0)
     * - El jugador todavía no tiró su carta
     * - El envido no fue resuelto aún
     */
    public boolean puedeCanarEnvido() {
        return rondaActual == 0 && !jugadorTiroCarta && estadoEnvido >= 0 && !envidoAceptado;
    }

    /**
     * True si la IA puede cantar envido:
     * - Solo en la primera ronda
     * - La IA todavía no tiró su carta
     */
    public boolean iaPuedeCanarEnvido() {
        return rondaActual == 0 && !iaTiroCarta && estadoEnvido >= 0 && !envidoAceptado && !envidoPendiente;
    }

    /** True si hay un envido pendiente iniciado por la IA que el jugador aún debe responder */
    public boolean isEnvidoPendienteDeIA() {
        return envidoPendiente && !trucoQuienCanto == false; // envido pendiente sin resolver
    }

    public static int calcularEnvido(ArrayList<Carta> mano) {
        String[] palos = {"Espada", "Oro", "Copa", "Basto"};
        int max = 0;
        for (String palo : palos) {
            ArrayList<Integer> nums = new ArrayList<>();
            for (Carta c : mano) {
                if (c.getPalo().equals(palo)) {
                    int n = c.getNumero();
                    nums.add(n <= 7 ? n : 0);
                }
            }
            int suma = 0;
            if (nums.size() >= 2) {
                for (int v : nums) suma += v;
                suma += 20;
            } else if (nums.size() == 1) {
                suma = nums.get(0);
            }
            if (suma > max) max = suma;
        }
        return max;
    }

    public String jugadorCantaEnvido(int nivel) {
        if (jugadorTiroCarta) return "El envido solo se puede cantar antes de tirar tu carta.";
        if (rondaActual > 0) return "El envido solo se puede cantar en la primera ronda.";
        if (envidoAceptado || estadoEnvido < 0) return "El envido ya fue jugado en esta mano.";
        if (nivel <= estadoEnvido && estadoEnvido > 0) return "Tenés que subir el canto (Real Envido o Falta Envido).";
        if (nivel == 2 && estadoEnvido == 0) return "Primero tenés que cantar Envido.";
        if (nivel == 3 && estadoEnvido == 0) return "Primero tenés que cantar Envido.";

        estadoEnvido    = nivel;
        envidoPendiente = true;

        return iaResponderEnvido();
    }

    /**
     * El jugador sube el envido directamente como respuesta a la IA (sin decir "Quiero" primero).
     */
    public String jugadorSubeEnvidoDirecto(int nuevoNivel) {
        if (!envidoPendiente) return null;
        if (nuevoNivel <= estadoEnvido) return null;
        if (jugadorTiroCarta || rondaActual > 0 || estadoEnvido < 0) return null;

        estadoEnvido    = nuevoNivel;
        envidoPendiente = true;

        return iaResponderEnvido();
    }

    private String iaResponderEnvido() {
        IA1.EvaluarPerceptrones();
        boolean acepta = (IA1.getSalidaEnvido() == 1);

        if (acepta) {
            envidoAceptado  = true;
            envidoPendiente = false;
            return resolverEnvido();
        } else {
            int pts = 1;
            Jugador1.setPuntaje(Jugador1.getPuntaje() + pts);
            envidoPendiente = false;
            estadoEnvido    = -1;
            return "IA: No quiero el envido. Vos ganás " + pts + " pto(s).";
        }
    }

    private String resolverEnvido() {
        int tantosJ  = calcularEnvido(Jugador1.getMano());
        int tantosIA = calcularEnvido(IA1.getMano());
        String res;

        if (estadoEnvido == 3) {
            int puntajeMaximo = 30;
            int puntosFalta = puntajeMaximo - Math.max(Jugador1.getPuntaje(), IA1.getPuntaje());
            if (puntosFalta < 1) puntosFalta = 1;
            if (tantosJ >= tantosIA) {
                Jugador1.setPuntaje(Jugador1.getPuntaje() + puntosFalta);
                res = "¡Ganaste el Falta Envido! Vos: " + tantosJ + " vs IA: " + tantosIA + ". +" + puntosFalta + " pts.";
            } else {
                IA1.setPuntaje(IA1.getPuntaje() + puntosFalta);
                res = "IA gana el Falta Envido. IA: " + tantosIA + " vs Vos: " + tantosJ + ". +" + puntosFalta + " pts para IA.";
            }
        } else {
            int pts = (estadoEnvido == 1) ? 2 : 3;
            if (tantosJ >= tantosIA) {
                Jugador1.setPuntaje(Jugador1.getPuntaje() + pts);
                res = "¡Ganaste el envido! Vos: " + tantosJ + " vs IA: " + tantosIA + ". +" + pts + " pts.";
            } else {
                IA1.setPuntaje(IA1.getPuntaje() + pts);
                res = "IA gana el envido. IA: " + tantosIA + " vs Vos: " + tantosJ + ". +" + pts + " pts para IA.";
            }
        }
        estadoEnvido = -1;
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANTOS INICIADOS POR LA IA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * La IA inicia el canto de Truco (nivel 1).
     * Devuelve el mensaje para mostrar al jugador, o null si no puede cantar.
     */
    public String iaCantaTruco() {
        if (trucoAceptado || trucoPendiente || estadoTruco > 0) return null;
        estadoTruco     = 1;
        trucoPendiente  = true;
        trucoQuienCanto = false; // la IA cantó → jugador debe responder
        puntosTruco     = 2;
        return "IA canta: ¡Truco! (" + puntosTruco + " pts en juego) — ¿Querés?";
    }

    /**
     * La IA inicia el canto de Envido (nivel 1).
     * Devuelve el mensaje para mostrar al jugador, o null si no puede cantar.
     */
    public String iaCantaEnvido() {
        if (!iaPuedeCanarEnvido()) return null;
        estadoEnvido    = 1;
        envidoPendiente = true;
        return "IA canta: ¡Envido! — ¿Querés?";
    }

    /**
     * El jugador responde al envido cantado por la IA.
     * Si acepta, NO resuelve inmediatamente — el jugador puede subir (Real Envido, Falta Envido).
     * Para cerrar el envido sin subir, el controlador llama resolverEnvidoFinal().
     */
    public String jugadorResponderEnvido(boolean acepta) {
        if (!envidoPendiente) return null;
        if (acepta) {
            // El jugador acepta pero puede subir — marcar como aceptado y dejar pendiente
            envidoAceptado  = true;
            envidoPendiente = false;
            // NO resolvemos todavía: el jugador puede subir con Real Envido / Falta Envido
            // El controlador debe mostrar los botones de subida y llamar resolverEnvidoFinal()
            // cuando el jugador decida no subir más (tira carta) o la IA no quiere subir.
            return "Aceptaste el Envido (" + puntosEnvido() + " pts en juego). ¿Subís?";
        } else {
            int pts = 1;
            IA1.setPuntaje(IA1.getPuntaje() + pts);
            envidoPendiente = false;
            estadoEnvido    = -1;
            return "No quisiste el envido. IA gana " + pts + " pto(s).";
        }
    }

    /** Puntos en juego por el envido según estado actual */
    private int puntosEnvido() {
        return estadoEnvido == 1 ? 2 : estadoEnvido == 2 ? 3 : 30;
    }

    /**
     * Cierra y resuelve el envido (cuando nadie sube más).
     * Se llama cuando el jugador tira carta sin subir, o la IA no quiere subir.
     */
    public String resolverEnvidoFinal() {
        if (estadoEnvido <= 0 && !envidoAceptado) return null;
        String res = resolverEnvido();
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Getters
    // ─────────────────────────────────────────────────────────────────────────
    public Jugador getJugador1()          { return Jugador1; }
    public IA      getIA1()               { return IA1; }
    public int     getEstadoTruco()       { return estadoTruco; }
    public boolean isTrucoPendiente()     { return trucoPendiente; }
    public boolean isTrucoAceptado()      { return trucoAceptado; }
    public boolean isTrucoQuienCanto()    { return trucoQuienCanto; }
    public int     getPuntosTruco()       { return puntosTruco; }
    public int     getEstadoEnvido()      { return estadoEnvido; }
    public boolean isEnvidoPendiente()    { return envidoPendiente; }
    public boolean isEnvidoAceptado()     { return envidoAceptado; }
}
