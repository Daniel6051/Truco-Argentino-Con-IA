package Modelo;

/**
 * Perceptrón Simple reutilizable para el proyecto JuegoTruco.
 * Basado en la arquitectura de PerceptronSimpleAND.java.
 * Cada instancia tiene sus propios pesos w0, w1, w2 y factor de aprendizaje.
 *
 * @author facun
 */
public class Perceptron {

    // Pesos sinápticos
    public double w0; // peso del bias
    public double w1; // peso entrada 1
    public double w2; // peso entrada 2

    // Factor de aprendizaje (tasa de aprendizaje)
    public double factorAprendizaje;

    // Últimos valores registrados (útiles para mostrar en la vista)
    public double ultimaEntrada1;
    public double ultimaEntrada2;
    public double ultimaSalidaNeta;
    public int    ultimaSalida;
    public double ultimoError;

    // Nombre descriptivo del perceptrón (para mostrar en la vista)
    public String nombre;

    /**
     * Constructor.
     * @param nombre           Nombre descriptivo (ej: "Envido", "Truco", "EstadoMano")
     * @param factorAprendizaje Tasa de aprendizaje (ej: 0.1)
     */
    public Perceptron(String nombre, double factorAprendizaje) {
        this.nombre           = nombre;
        this.factorAprendizaje = factorAprendizaje;
        // Pesos iniciales aleatorios pequeños, igual que PerceptronSimpleAND
        this.w0 = Math.random() * 0.2 - 0.1;
        this.w1 = Math.random() * 0.2 - 0.1;
        this.w2 = Math.random() * 0.2 - 0.1;
    }

    /**
     * Función de activación escalón bipolar: +1 si net >= 0, -1 si net < 0.
     */
    private int FuncionActivacion(double net) {
        return (net >= 0) ? 1 : -1;
    }

    /**
     * Calcula la salida neta: net = w0*(-1) + w1*x1 + w2*x2
     * (bias negativo, igual que PerceptronSimpleAND)
     */
    private double SalidaNeta(double x1, double x2) {
        return w0 * (-1) + w1 * x1 + w2 * x2;
    }

    /**
     * Evalúa el perceptrón con las entradas dadas.
     * Guarda los valores internos para visualización.
     * @param x1 Entrada 1 (normalizada, ej: -1 o +1)
     * @param x2 Entrada 2 (normalizada, ej: -1 o +1)
     * @return   Salida: +1 o -1
     */
    public int Evaluar(double x1, double x2) {
        ultimaEntrada1  = x1;
        ultimaEntrada2  = x2;
        ultimaSalidaNeta = SalidaNeta(x1, x2);
        ultimaSalida    = FuncionActivacion(ultimaSalidaNeta);
        ultimoError     = 0; // sin error en evaluación pura
        return ultimaSalida;
    }

    /**
     * Realiza UNA iteración de aprendizaje supervisado.
     * Ajusta los pesos si la salida difiere del valor esperado.
     * @param x1       Entrada 1
     * @param x2       Entrada 2
     * @param esperado Salida deseada (+1 o -1)
     * @return         Error calculado (esperado - obtenido)
     */
    public double Aprendizaje(double x1, double x2, int esperado) {
        double net    = SalidaNeta(x1, x2);
        int    salida = FuncionActivacion(net);
        double error  = esperado - salida;

        // Actualizar pesos sólo si hay error (igual que PerceptronSimpleAND)
        if (error != 0) {
            w0 = w0 + factorAprendizaje * error * (-1);
            w1 = w1 + factorAprendizaje * error * x1;
            w2 = w2 + factorAprendizaje * error * x2;
        }

        // Guardar para visualización
        ultimaEntrada1   = x1;
        ultimaEntrada2   = x2;
        ultimaSalidaNeta = net;
        ultimaSalida     = salida;
        ultimoError      = error;

        return error;
    }

    /**
     * Entrenamiento completo con un conjunto de ejemplos.
     * Itera hasta que el error total sea 0 o se alcancen maxEpocas.
     * @param ejemplos    Matriz Nx2 de entradas
     * @param etiquetas   Vector N de salidas esperadas (+1 o -1)
     * @param maxEpocas   Límite de épocas para evitar bucle infinito
     */
    public void Entrenamiento(double[][] ejemplos, int[] etiquetas, int maxEpocas) {
        for (int epoca = 0; epoca < maxEpocas; epoca++) {
            double errorTotal = 0;
            for (int i = 0; i < ejemplos.length; i++) {
                errorTotal += Math.abs(Aprendizaje(ejemplos[i][0], ejemplos[i][1], etiquetas[i]));
            }
            if (errorTotal == 0) break; // convergió
        }
    }

    /**
     * Prueba de funcionamiento: evalúa todos los ejemplos e imprime resultados.
     * Igual al método PruebaFuncionamiento de PerceptronSimpleAND.
     * @param ejemplos  Entradas de prueba
     * @param etiquetas Salidas esperadas
     * @return          true si clasifica correctamente todos los ejemplos
     */
    public boolean PruebaFuncionamiento(double[][] ejemplos, int[] etiquetas) {
        boolean todoOk = true;
        System.out.println("=== Prueba: " + nombre + " ===");
        for (int i = 0; i < ejemplos.length; i++) {
            int salida = Evaluar(ejemplos[i][0], ejemplos[i][1]);
            System.out.println("Entrada [" + ejemplos[i][0] + ", " + ejemplos[i][1] + "] "
                    + "-> Salida: " + salida + " | Esperado: " + etiquetas[i]);
            if (salida != etiquetas[i]) todoOk = false;
        }
        System.out.println("Resultado: " + (todoOk ? "CORRECTO" : "INCORRECTO"));
        return todoOk;
    }

    // -------------------------------------------------------------------------
    // Getters para la vista
    // -------------------------------------------------------------------------

    public double getW0() { return w0; }
    public double getW1() { return w1; }
    public double getW2() { return w2; }
    public double getUltimaEntrada1()  { return ultimaEntrada1; }
    public double getUltimaEntrada2()  { return ultimaEntrada2; }
    public double getUltimaSalidaNeta(){ return ultimaSalidaNeta; }
    public int    getUltimaSalida()    { return ultimaSalida; }
    public double getUltimoError()     { return ultimoError; }
    public String getNombre()          { return nombre; }
}
