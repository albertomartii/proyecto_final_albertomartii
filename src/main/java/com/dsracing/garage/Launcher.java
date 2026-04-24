package com.dsracing.garage;

/**
 * Clase de arranque separada de GarageApplication.
 *
 * Esto es NECESARIO cuando se combina Spring Boot (fat JAR) con JavaFX.
 * El problema: el Manifest del fat JAR apunta directamente a la clase
 * principal, pero JavaFX requiere que el main NO extienda Application
 * para poder inicializar el toolkit correctamente.
 *
 * Solución estándar: esta clase es el punto de entrada real (Main-Class
 * en el JAR), y delega a GarageApplication.main() que sí extiende Application.
 */
public class Launcher {
    public static void main(String[] args) {
        GarageApplication.main(args);
    }
}