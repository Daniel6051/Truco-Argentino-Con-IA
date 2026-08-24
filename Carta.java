/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Modelo;

import javax.swing.ImageIcon;

/**
 *
 * @author facun
 */
public class Carta {
    private String palo;
    private int numero;
    private int valor; 
     private String rutaImagen;

    public Carta(String palo, int numero) {
        this.palo = palo;
        this.numero = numero;
        this.valor = asignarValor(numero, palo); 
         this.rutaImagen = "/Imagenes/" + numero + "De" + palo + ".jpg"; 
    }

    public Carta() {}

    public String getPalo() {
        return palo;
    }


    public int getNumero() {
        return numero;
    }



    public int getValor() {
        return this.valor;
    }

  

  
    private int asignarValor(int numero, String palo) {
        if (numero == 1 && palo.equals("Espada")) return 14;
        if (numero == 1 && palo.equals("Basto")) return 13;
        if (numero == 7 && palo.equals("Espada")) return 12;
        if (numero == 7 && palo.equals("Oro")) return 11;
        if (numero == 3) return 10;
        if (numero == 2) return 9;
        if (numero == 1) return 8;
        if (numero == 12) return 7;
        if (numero == 11) return 6;
        if (numero == 10) return 5;
        if (numero == 7) return 4;
        if (numero == 6) return 3;
        if (numero == 5) return 2;
        if (numero == 4) return 1;
        return 0; 
    }
      public String getRutaImagen() {
        return rutaImagen;
    }

    
     public ImageIcon getImagen() {
        
        return new ImageIcon(getClass().getResource(rutaImagen));
    }
}
