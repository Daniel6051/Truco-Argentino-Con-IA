/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Modelo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
/**
 *
 * @author facun
 */
public class modelo {
    public Juego juego ;
    public modelo() {
        this.juego = new Juego();
    }
    
    public void Iniciar(){
      
    }
    
    public StringBuilder LeerReglas() throws FileNotFoundException {
    StringBuilder texto = new StringBuilder();

    try (InputStream inputStream = getClass().getResourceAsStream("/Modelo/Reglas.txt")) {
        if (inputStream == null) {
            throw new FileNotFoundException("Archivo Reglas.txt no encontrado en el classpath.");
        }

        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNextLine()) {
            texto.append(scanner.nextLine()).append("\n");
        }
        scanner.close();
    } catch (IOException e) {
        System.err.println("Error al leer el archivo: " + e.getMessage());
    }

    return texto;
}
}
