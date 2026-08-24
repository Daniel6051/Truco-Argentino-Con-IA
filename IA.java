package Modelo;

import java.util.ArrayList;
import java.util.Random;

/**
 * IA mejorada con 3 Perceptrones Simples en arquitectura MVC.
 *
 * Perceptrón A – "Envido"    : evalúa poder de envido (≥27 → +1, si no → -1).
 * Perceptrón B – "Truco"     : evalúa jerarquía de cartas (valor ≥11 → +1, si no → -1).
 * Perceptrón C – "EstadoMano": evalúa si ganó la ronda anterior (+1 si ganó, -1 si no).
 * Neurona AND final           : combina las 3 salidas para decidir la jugada.
 *
 * @author facun
 */
public class IA extends Jugador {

    // ── Tres perceptrones ────────────────────────────────────────────────────
    public Perceptron perceptronEnvido;
    public Perceptron perceptronTruco;
    public Perceptron perceptronEstado;

    // ── Últimas salidas de cada perceptrón (para la vista) ───────────────────
    public int salidaEnvido;
    public int salidaTruco;
    public int salidaEstado;
    public int salidaDecision; // resultado AND final

    // ── Estado interno del juego ─────────────────────────────────────────────
    private boolean ganoRondaAnterior;

    // ── Datos de entrenamiento de cada perceptrón ────────────────────────────
    // Perceptrón A – Envido: x1 = tantos/33 normalizado, x2 = bias constante +1
    // Umbral bajado: ≥24 tantos → quiere envido (antes era ≥27, muy exigente)
    private static final double[][] TRAIN_ENVIDO = {
        {0.73, 1}, {0.82, 1}, {0.91, 1}, {1.00, 1},  // ≥24 tantos → +1
        {0.30, 1}, {0.45, 1}, {0.55, 1}, {0.24, 1}   // <24 tantos → -1
    };
    private static final int[] LABEL_ENVIDO = {1, 1, 1, 1, -1, -1, -1, -1};

    // Perceptrón B – Truco: x1 = valorMejorCarta/14 normalizado, x2 = bias +1
    // Umbral bajado: valor ≥8 (normalizado ≥0.57) → quiere truco (antes era ≥11, muy exigente)
    private static final double[][] TRAIN_TRUCO = {
        {0.57, 1}, {0.64, 1}, {0.79, 1}, {0.86, 1}, {1.00, 1}, {0.93, 1},  // valor ≥8  → +1
        {0.21, 1}, {0.29, 1}, {0.36, 1}, {0.43, 1}                          // valor <7  → -1
    };
    private static final int[] LABEL_TRUCO = {1, 1, 1, 1, 1, 1, -1, -1, -1, -1};

    // Perceptrón C – EstadoMano: x1 = 1 si ganó / -1 si no, x2 = bias +1
    private static final double[][] TRAIN_ESTADO = {
        { 1, 1}, { 1, 1}, { 1, 1},   // ganó  → +1
        {-1, 1}, {-1, 1}, {-1, 1}    // perdió → -1
    };
    private static final int[] LABEL_ESTADO = {1, 1, 1, -1, -1, -1};

    // ── Constructor ──────────────────────────────────────────────────────────
    public IA() {
        this.puntaje           = 0;
        this.ganoRondaAnterior = false;

        // Crear los tres perceptrones con factor de aprendizaje 0.1
        perceptronEnvido = new Perceptron("Envido",     0.1);
        perceptronTruco  = new Perceptron("Truco",      0.1);
        perceptronEstado = new Perceptron("EstadoMano", 0.1);

        // Entrenar cada perceptrón con su conjunto de datos
        perceptronEnvido.Entrenamiento(TRAIN_ENVIDO, LABEL_ENVIDO, 1000);
        perceptronTruco .Entrenamiento(TRAIN_TRUCO,  LABEL_TRUCO,  1000);
        perceptronEstado.Entrenamiento(TRAIN_ESTADO, LABEL_ESTADO, 1000);
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Evalúa los 3 perceptrones con la mano actual y decide la jugada.
     * Guarda las salidas individuales para que el controlador las muestre.
     */
    public void EvaluarPerceptrones() {
        // ── Perceptrón A: Envido ─────────────────────────────────────────────
        double tantos      = calcularTantos();
        double x1Envido    = tantos / 33.0; // normalizar sobre el máximo posible
        salidaEnvido       = perceptronEnvido.Evaluar(x1Envido, 1.0);

        // ── Perceptrón B: Truco ──────────────────────────────────────────────
        double mejorValor  = calcularMejorValorCarta();
        double x1Truco     = mejorValor / 14.0; // normalizar sobre el máximo (14)
        salidaTruco        = perceptronTruco.Evaluar(x1Truco, 1.0);

        // ── Perceptrón C: Estado de mano ─────────────────────────────────────
        double x1Estado    = ganoRondaAnterior ? 1.0 : -1.0;
        salidaEstado       = perceptronEstado.Evaluar(x1Estado, 1.0);

        // ── Neurona AND: decisión final ──────────────────────────────────────
        // Suma de las 3 salidas: si las 3 son +1 la suma es 3; umbral ≥ 2 (mayoría)
        int suma           = salidaEnvido + salidaTruco + salidaEstado;
        salidaDecision     = (suma >= 2) ? 1 : -1;
    }

    /**
     * Informa al perceptrón de estado si la IA ganó la ronda.
     */
    public void setGanoRonda(boolean gano) {
        this.ganoRondaAnterior = gano;
    }

    // ── Lógica de selección de carta ────────────────────────────────────────

    /**
     * Selecciona carta usando la decisión del perceptrón.
     * Si salidaDecision == +1: juega agresivo (la mejor carta que gane al rival).
     * Si salidaDecision == -1: juega conservador (la carta más baja).
     */
    public Carta Seleccionarcarta(Carta cartarival) {
        // Evaluar perceptrones antes de decidir
        EvaluarPerceptrones();

        if (cartarival != null) {
            int    valorRival    = cartarival.getValor();
            Carta  cartaParaGanar = null;
            Carta  cartaMasBaja   = null;

            for (Carta carta : getMano()) {
                if (cartaMasBaja == null || carta.getValor() < cartaMasBaja.getValor()) {
                    cartaMasBaja = carta;
                }
                if (carta.getValor() > valorRival &&
                    (cartaParaGanar == null || carta.getValor() < cartaParaGanar.getValor())) {
                    cartaParaGanar = carta;
                }
            }

            if (salidaDecision == 1) {
                // Agresivo: intentar ganar
                cartaElegida = (cartaParaGanar != null) ? cartaParaGanar : cartaMasBaja;
            } else {
                // Conservador: gastar la más baja si no puede ganar
                cartaElegida = (cartaMasBaja != null) ? cartaMasBaja : cartaParaGanar;
            }

        } else {
            // Sin carta rival: salida perceptrón decide si jugar la mejor o una aleatoria
            if (salidaDecision == 1) {
                cartaElegida = getMejorCarta();
            } else {
                Random random = new Random();
                cartaElegida  = getMano().get(random.nextInt(getMano().size()));
            }
        }

        getMano().remove(cartaElegida);
        return cartaElegida;
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /** Calcula la suma de tantos de envido de la mano. */
    public double calcularTantos() {
        // Por palo: sumar los valores de envido (cartas 1-7 valen su número; figuras valen 0)
        int[] tantosPorPalo = new int[4];
        String[] palos = {"Espada", "Oro", "Copa", "Basto"};

        for (int p = 0; p < palos.length; p++) {
            int suma = 0;
            int count = 0;
            for (Carta c : getMano()) {
                if (c.getPalo().equals(palos[p])) {
                    int n = c.getNumero();
                    suma += (n <= 7) ? n : 0;
                    count++;
                }
            }
            tantosPorPalo[p] = (count >= 2) ? suma + 20 : suma;
        }

        int max = 0;
        for (int t : tantosPorPalo) if (t > max) max = t;
        return max;
    }

    /** Devuelve el valor más alto de la mano según jerarquía de Truco. */
    public double calcularMejorValorCarta() {
        int max = 0;
        for (Carta c : getMano()) {
            if (c.getValor() > max) max = c.getValor();
        }
        return max;
    }

    /** Devuelve la carta de mayor valor jerárquico de la mano. */
    private Carta getMejorCarta() {
        Carta mejor = getMano().get(0);
        for (Carta c : getMano()) {
            if (c.getValor() > mejor.getValor()) mejor = c;
        }
        return mejor;
    }

    // ── Overrides heredados ──────────────────────────────────────────────────

    @Override
    public ArrayList<Carta> getMano() {
        return Mano;
    }

    @Override
    public int getPuntaje() {
        return puntaje;
    }

    public Carta getCartaElegida() {
        return cartaElegida;
    }

    public void setMano(ArrayList<Carta> Mano) {
        this.Mano = Mano;
    }

    @Override
    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }

    public void setCartaElegida(Carta cartaElegida) {
        this.cartaElegida = cartaElegida;
    }

    // ── Getters de estado para la vista ─────────────────────────────────────
    public int getSalidaEnvido()    { return salidaEnvido; }
    public int getSalidaTruco()     { return salidaTruco; }
    public int getSalidaEstado()    { return salidaEstado; }
    public int getSalidaDecision()  { return salidaDecision; }
    public boolean isGanoRondaAnterior() { return ganoRondaAnterior; }
}
