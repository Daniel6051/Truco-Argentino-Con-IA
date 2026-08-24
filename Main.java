
package Controlador;

import Modelo.modelo;
import Vista.Menu;

/**
 *
 * @author facun
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    Menu v1=new Menu();
    modelo m1=new modelo();
    controlador c1=new controlador(m1,v1);
    
    }
    
}
