package Modelo;

import java.util.ArrayList;

/**
 * Jugador con selección de carta por índice real en la mano.
 */
public class Jugador {
    protected ArrayList<Carta> Mano;
    protected int puntaje;
    protected Carta cartaElegida;

    public Jugador() {
        this.Mano = new ArrayList<>();
        this.puntaje = 0;
        this.cartaElegida = new Carta();
    }

    public ArrayList<Carta> getMano() { return this.Mano; }
    public int getPuntaje() { return this.puntaje; }

    public void setMano(Carta mano) {
        this.Mano.add(mano);
    }

    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }
    public Carta getCartaElegida() { return this.cartaElegida; }
    public void setCartaElegida(Carta c) { this.cartaElegida = c; }

    /**
     * Selecciona y remueve la carta en la posición indicada (0-based) del ArrayList Mano.
     * El controlador calcula el índice correcto contando botones visibles.
     */
    public void SeleccionarcartaPorIndice(int indiceMano) {
        if (this.Mano.isEmpty()) return;
        if (indiceMano < 0) indiceMano = 0;
        if (indiceMano >= this.Mano.size()) indiceMano = this.Mano.size() - 1;
        cartaElegida = this.Mano.get(indiceMano);
        this.Mano.remove(indiceMano);
    }

    /** Compatibilidad: opcion 1-based */
    public void Seleccionarcarta(int opcion) {
        SeleccionarcartaPorIndice(opcion - 1);
    }
}
