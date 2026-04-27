package com.dsracing.garage;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.repository.CarRepository;
import com.dsracing.garage.service.GarageService;
import com.dsracing.garage.service.impl.DynoService;
import com.dsracing.garage.ui.fx.controllers.DynoController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.dsracing.garage.service.CarService;
import com.dsracing.garage.service.UserService;
import com.dsracing.garage.ui.fx.controllers.LoginController;
import java.util.Optional;

/**
 * Punto de entrada que combina Spring Boot + JavaFX.
 *
 * Flujo:
 *  1. JavaFX llama a start(Stage)
 *  2. start() arranca Spring en un hilo separado
 *  3. Cuando Spring termina de inicializarse, se lanza la ventana del dyno
 */
@SpringBootApplication
public class GarageApplication extends Application {

    private ConfigurableApplicationContext springContext;

    // ── Punto de entrada real (JavaFX necesita main sin @SpringBootApplication) ──
    public static void main(String[] args) {
        // Lanzar JavaFX (que internamente llamará a start())
        Application.launch(GarageApplication.class, args);
    }

    @Override
    public void init() {
        // Arrancar Spring Boot en el hilo de inicialización de JavaFX
        // (NO en el hilo de JavaFX para no bloquearlo)
        springContext = SpringApplication.run(GarageApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        UserService userService = springContext.getBean(UserService.class);
        CarService  carService  = springContext.getBean(CarService.class);
        DynoService dynoService = springContext.getBean(DynoService.class);

        GarageService garageService = springContext.getBean(GarageService.class);
        LoginController loginController = new LoginController(userService, carService, dynoService, garageService);
        loginController.show(primaryStage);
    }

    @Override
    public void stop() {
        // Cerrar Spring limpiamente al cerrar la ventana JavaFX
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}