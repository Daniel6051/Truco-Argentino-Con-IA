/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Modelo;

import java.util.ArrayList;
import java.util.Stack;

/**
 *
 * @author facun
 */
public class Mazo {
    private ArrayList <Carta> cartas;
    private Stack <Carta> CartaMixed;
    
    public Mazo(){
       this.cartas = new ArrayList<>();
        String[] palos = {"Espada", "Oro", "Copa", "Basto"};
        for (String palo : palos) {
            for (int i = 1; i <= 12; i++) {
                if (i != 8 && i != 9)  
                    cartas.add(new Carta(palo, i));
                
            }
        }
   } 
 public void mezclar() {
  
    if (CartaMixed == null) {
        this.CartaMixed = new Stack<>();
    }

    int minimo = 0;
    int maximo = 39;

    boolean[] seleccionadas = new boolean[cartas.size()];

    while (CartaMixed.size() < cartas.size()) {
        int numeroAleatorio = (int) (Math.random() * (maximo - minimo + 1)) + minimo;

        if (!seleccionadas[numeroAleatorio]) {
            CartaMixed.push(cartas.get(numeroAleatorio));
            seleccionadas[numeroAleatorio] = true;
        }
    }
}

    public ArrayList<Carta> getCartas() {
        return cartas;
    }

    public Stack<Carta> getCartaMixed() {
        return CartaMixed;
    }

   } 

