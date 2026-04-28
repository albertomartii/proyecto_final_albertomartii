package com.dsracing.garage.ui.fx.controllers;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.Garage;
import com.dsracing.garage.model.entity.User;
import com.dsracing.garage.repository.PartRepository;
import com.dsracing.garage.service.CarService;
import com.dsracing.garage.service.GarageService;
import com.dsracing.garage.service.UserService;
import com.dsracing.garage.service.impl.DynoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class LoginController {

    // ── Colores ───────────────────────────────────────────────────────────────
    private static final String BG_DARK    = "#0d0d0f";
    private static final String BG_PANEL   = "#13131a";
    private static final String ACCENT_RED = "#e8002d";
    private static final String ACCENT_YEL = "#f5c400";
    private static final String TEXT_WHITE = "#f0f0f0";
    private static final String TEXT_GRAY  = "#666680";
    private static final String BORDER     = "#2a2a3a";

    // ── Coches de serie ───────────────────────────────────────────────────────
    private static final String[][] STARTER_CARS = {
            {"Nissan",  "S13",          "1992", "200", "250", "1200", "1.0", "0.50"},
            {"Honda",   "Civic Type R", "2001", "185", "190", "1100", "1.0", "0.62"},
            {"Subaru",  "Impreza WRX",  "2003", "230", "310", "1400", "1.0", "0.55"},
            {"Citröen", "C4 Coupé", "2025", "150", "250", "1230", "2.0", "0.70"},
            {"BMW", "E39", "1999", "193", "200", "1500", "1.0", "0.50"}
    };

    // ── Dependencias ──────────────────────────────────────────────────────────
    private Stage        stage;
    private User         currentUser;
    private final UserService    userService;
    private final CarService     carService;
    private final DynoService    dynoService;
    private final GarageService  garageService;
    private final PartRepository partRepository; // ← NUEVO

    public LoginController(UserService userService,
                           CarService carService,
                           DynoService dynoService,
                           GarageService garageService,
                           PartRepository partRepository) {  // ← NUEVO parámetro
        this.userService    = userService;
        this.carService     = carService;
        this.dynoService    = dynoService;
        this.garageService  = garageService;
        this.partRepository = partRepository;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Mostrar pantalla de login
    // ════════════════════════════════════════════════════════════════════════

    public void show(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("DS Racing · Login");
        stage.setScene(buildLoginScene());
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.setWidth(1920);
        stage.setHeight(1080);
        stage.centerOnScreen();
        stage.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Escena de Login
    // ════════════════════════════════════════════════════════════════════════

    private Scene buildLoginScene() {
        Label dsLabel = new Label("DS");
        dsLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 52));
        dsLabel.setTextFill(Color.web(ACCENT_RED));

        Label racingLabel = new Label("RACING");
        racingLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 52));
        racingLabel.setTextFill(Color.web(TEXT_WHITE));

        HBox titleBox = new HBox(8, dsLabel, racingLabel);
        titleBox.setAlignment(Pos.CENTER);

        Label subtitle = new Label("GARAGE · MANAGEMENT SYSTEM");
        subtitle.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));
        subtitle.setTextFill(Color.web(TEXT_GRAY));

        VBox headerBox = new VBox(4, titleBox, subtitle);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 32, 0));

        Label userLabel = makeFieldLabel("USUARIO");
        TextField userField = makeTextField("Introduce tu usuario");

        Label passLabel = makeFieldLabel("CONTRASEÑA");
        PasswordField passField = makePasswordField("Introduce tu contraseña");

        Label errorLabel = new Label("");
        errorLabel.setFont(Font.font("Monospace", 11));
        errorLabel.setTextFill(Color.web(ACCENT_RED));
        errorLabel.setMinHeight(18);

        Button btnLogin    = makeButton("INICIAR SESIÓN", ACCENT_RED);
        Button btnRegister = makeButton("CREAR CUENTA", BG_PANEL);
        btnRegister.setStyle(btnRegister.getStyle() +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;");

        btnLogin.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Rellena todos los campos.");
                return;
            }
            Optional<User> result = userService.login(user, pass);
            if (result.isPresent()) {
                showGarageScene(result.get());
            } else {
                errorLabel.setText("Usuario o contraseña incorrectos.");
                passField.clear();
            }
        });

        btnRegister.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (user.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Rellena todos los campos.");
                return;
            }
            if (userService.existsByUsername(user)) {
                errorLabel.setText("Ese usuario ya existe.");
                return;
            }
            User newUser = userService.register(user, pass, user + "@dsracing.com");
            showGarageScene(newUser);
        });

        passField.setOnAction(e -> btnLogin.fire());

        VBox form = new VBox(10,
                userLabel, userField,
                passLabel, passField,
                errorLabel,
                btnLogin, btnRegister);
        form.setMaxWidth(360);
        form.setPadding(new Insets(28));
        form.setStyle("-fx-background-color:" + BG_PANEL + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        VBox root = new VBox(headerBox, form);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(0);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color:" + BG_DARK + ";");

        return new Scene(root, 520, 520);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Escena del Garaje
    // ════════════════════════════════════════════════════════════════════════

    private void showGarageScene(User user) {
        this.currentUser = user;
        List<Car> myCars = carService.findByUserId(user.getId());

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color:" + BG_DARK + ";");
        root.setPadding(new Insets(40));

        Label welcome = new Label("BIENVENIDO, " + user.getUsername().toUpperCase());
        welcome.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        welcome.setTextFill(Color.web(ACCENT_RED));

        Label garageTitle = new Label("MI GARAJE");
        garageTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        garageTitle.setTextFill(Color.web(TEXT_WHITE));

        root.getChildren().addAll(welcome, garageTitle, makeDivider());

        if (!myCars.isEmpty()) {
            root.getChildren().add(makeFieldLabel("MIS COCHES"));
            for (Car car : myCars) {
                root.getChildren().add(buildCarCard(car, user));
            }
            root.getChildren().add(makeDivider());
        }

        Label newBuildLabel = makeFieldLabel(
                myCars.isEmpty() ? "ELIGE TU PRIMER COCHE" : "+ CREAR NUEVA BUILD");
        root.getChildren().add(newBuildLabel);
        for (String[] carData : STARTER_CARS) {
            root.getChildren().add(buildStarterCarCard(carData, user));
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + BG_DARK + "; -fx-background:" + BG_DARK + ";");

        stage.setScene(new Scene(scroll, 760, 620));
        stage.setResizable(true);
        stage.setTitle("DS Racing · Garaje de " + user.getUsername());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tarjeta de coche existente
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildCarCard(Car car, User user) {
        Label name = new Label(car.getMake().toUpperCase() + "  " + car.getModel().toUpperCase());
        name.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        name.setTextFill(Color.web(TEXT_WHITE));

        Label yearLbl = new Label(String.valueOf(car.getYear()));
        yearLbl.setFont(Font.font("Monospace", 12));
        yearLbl.setTextFill(Color.web(TEXT_GRAY));

        double hp = car.getBasePower(), torque = car.getBaseTorque(), mass = car.getMass();
        if (car.getParts() != null) {
            for (var p : car.getParts()) {
                hp     += p.getHpDelta();
                torque += p.getTorqueDelta();
                mass   += p.getWeightDelta();
            }
        }
        Label stats = new Label(String.format("%.0f HP  ·  %.0f Nm  ·  %.0f kg", hp, torque, mass));
        stats.setFont(Font.font("Monospace", 11));
        stats.setTextFill(Color.web(TEXT_GRAY));

        int partsCount = car.getParts() != null ? car.getParts().size() : 0;
        Label partsBadge = new Label(partsCount + " pieza" + (partsCount != 1 ? "s" : "") +
                " instalada" + (partsCount != 1 ? "s" : ""));
        partsBadge.setFont(Font.font("Monospace", 9));
        partsBadge.setTextFill(Color.web(partsCount > 0 ? "#00e676" : TEXT_GRAY));

        VBox info = new VBox(4, name, yearLbl, stats, partsBadge);
        info.setAlignment(Pos.CENTER_LEFT);

        // ── Botón PIEZAS ──────────────────────────────────────────────────────
        Button btnParts = new Button("⚙  PIEZAS");
        btnParts.setStyle(
                "-fx-background-color:#1a1a2a; -fx-text-fill:#00c8ff;" +
                        "-fx-border-color:#00c8ff; -fx-border-width:1;" +
                        "-fx-font-family:Monospace; -fx-font-weight:bold; -fx-font-size:11;" +
                        "-fx-padding:9 16; -fx-border-radius:4; -fx-background-radius:4; -fx-cursor:hand;");
        btnParts.setOnAction(e -> openPartEditor(car, user));

        // ── Botón DYNO TEST ───────────────────────────────────────────────────
        Button btnDyno = makeButton("▶  DYNO TEST", ACCENT_RED);
        btnDyno.setMaxWidth(140);
        btnDyno.setOnAction(e -> launchDyno(car));

        // ── Botón BORRAR ──────────────────────────────────────────────────────
        Button btnDelete = new Button("✕");
        btnDelete.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#444460;" +
                        "-fx-border-color:transparent; -fx-font-size:16;" +
                        "-fx-padding:4 8; -fx-cursor:hand;");
        btnDelete.setOnMouseEntered(e ->
                btnDelete.setStyle(
                        "-fx-background-color:#2a0010; -fx-text-fill:" + ACCENT_RED + ";" +
                                "-fx-border-color:" + ACCENT_RED + "; -fx-border-width:1;" +
                                "-fx-border-radius:4; -fx-background-radius:4;" +
                                "-fx-font-size:16; -fx-padding:4 8; -fx-cursor:hand;"));
        btnDelete.setOnMouseExited(e ->
                btnDelete.setStyle(
                        "-fx-background-color:transparent; -fx-text-fill:#5f5f6a;" +
                                "-fx-border-color:transparent; -fx-font-size:16;" +
                                "-fx-padding:4 8; -fx-cursor:hand;"));
        btnDelete.setOnAction(e -> {
            // Diálogo de confirmación antes de borrar
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Borrar coche");
            confirm.setHeaderText(car.getMake() + " " + car.getModel() + " (" + car.getYear() + ")");
            confirm.setContentText("¿Seguro que quieres borrar este coche? Esta acción no se puede deshacer.");
            confirm.getDialogPane().setStyle("-fx-background-color:#13131a; -fx-font-family:Monospace;");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    carService.deleteById(car.getId());
                    showGarageScene(user); // recargar el garaje
                }
            });
        });

        // ── Layout de botones: papelera arriba a la derecha, PIEZAS y DYNO abajo ──
        VBox buttons = new VBox(8, btnParts, btnDyno);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox rightBox = new VBox(4, btnDelete, buttons);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        HBox card = new HBox(info, new Region(), rightBox);
        HBox.setHgrow(info, Priority.ALWAYS);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:" + BG_PANEL + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Abrir editor de piezas — CORREGIDO: pasa partRepository
    // ════════════════════════════════════════════════════════════════════════

    private void openPartEditor(Car car, User user) {
        PartEditorController editor = new PartEditorController();
        editor.show(car, carService, dynoService, partRepository, savedCar -> {
            // Recargar el garaje para reflejar los cambios guardados
            showGarageScene(user);
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tarjeta de coche starter
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildStarterCarCard(String[] data, User user) {
        String make   = data[0];
        String model  = data[1];
        String year   = data[2];
        double hp     = Double.parseDouble(data[3]);
        double torque = Double.parseDouble(data[4]);
        double mass   = Double.parseDouble(data[5]);

        Label name = new Label(make.toUpperCase() + "  " + model.toUpperCase());
        name.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        name.setTextFill(Color.web(TEXT_WHITE));

        Label yearLabel = new Label(year);
        yearLabel.setFont(Font.font("Monospace", 12));
        yearLabel.setTextFill(Color.web(TEXT_GRAY));

        Label stats = new Label(String.format("%.0f HP  ·  %.0f Nm  ·  %.0f kg", hp, torque, mass));
        stats.setFont(Font.font("Monospace", 11));
        stats.setTextFill(Color.web(TEXT_GRAY));

        HBox statBars = buildStatBars(hp, torque, mass);

        VBox info = new VBox(4, name, yearLabel, stats, statBars);
        info.setAlignment(Pos.CENTER_LEFT);

        Button btn = makeButton("ELEGIR", ACCENT_YEL);
        btn.setStyle(btn.getStyle() + "-fx-text-fill:#0d0d0f;");
        btn.setOnAction(e -> selectStarterCar(data, user));

        HBox card = new HBox(info, new Region(), btn);
        HBox.setHgrow(info, Priority.ALWAYS);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:" + BG_PANEL + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#1a1a25; -fx-border-color:" + ACCENT_RED +
                        "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:" + BG_PANEL + "; -fx-border-color:" + BORDER +
                        "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;"));

        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lógica
    // ════════════════════════════════════════════════════════════════════════

    private void selectStarterCar(String[] data, User user) {
        Car car = new Car();
        car.setMake(data[0]);
        car.setModel(data[1]);
        car.setYear(Integer.parseInt(data[2]));
        car.setBasePower(Double.parseDouble(data[3]));
        car.setBaseTorque(Double.parseDouble(data[4]));
        car.setMass(Double.parseDouble(data[5]));
        car.setGripBase(Double.parseDouble(data[6]));
        car.setWeightDistributionFront(Double.parseDouble(data[7]));

        Garage garage = garageService.getOrCreateGarageForUser(user);
        car.setGarage(garage);

        Car saved = carService.save(car);
        // Recargar el garaje para mostrar el coche recién añadido
        showGarageScene(user);
    }

    private void launchDyno(Car car) {
        DynoController dynoController = new DynoController();
        dynoController.show(car, dynoService);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de estilo
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildStatBars(double hp, double torque, double mass) {
        HBox bars = new HBox(12);
        bars.setAlignment(Pos.CENTER_LEFT);
        bars.setPadding(new Insets(4, 0, 0, 0));
        bars.getChildren().addAll(
                buildBar("POT", hp / 400.0, ACCENT_RED),
                buildBar("PAR", torque / 500.0, "#00c8ff"),
                buildBar("PES", 1.0 - (mass / 1600.0), ACCENT_YEL));
        return bars;
    }

    private VBox buildBar(String label, double pct, String color) {
        pct = Math.max(0, Math.min(1, pct));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Monospace", 9));
        lbl.setTextFill(Color.web(TEXT_GRAY));

        Pane bg = new Pane();
        bg.setPrefSize(60, 5);
        bg.setStyle("-fx-background-color:#1e1e2e; -fx-background-radius:3;");

        Pane fill = new Pane();
        fill.setPrefSize(60 * pct, 5);
        fill.setStyle("-fx-background-color:" + color + "; -fx-background-radius:3;");

        StackPane barPane = new StackPane(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        return new VBox(2, lbl, barPane);
    }

    private Label makeFieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(TEXT_GRAY));
        l.setPadding(new Insets(4, 0, 0, 0));
        return l;
    }

    private TextField makeTextField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder);
        tf.setStyle("-fx-background-color:#0d0d0f; -fx-text-fill:" + TEXT_WHITE + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;" +
                "-fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-family:Monospace; -fx-font-size:13; -fx-padding:8 12;");
        return tf;
    }

    private PasswordField makePasswordField(String placeholder) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(placeholder);
        pf.setStyle("-fx-background-color:#0d0d0f; -fx-text-fill:" + TEXT_WHITE + ";" +
                "-fx-border-color:" + BORDER + "; -fx-border-width:1;" +
                "-fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-font-family:Monospace; -fx-font-size:13; -fx-padding:8 12;");
        return pf;
    }

    private Button makeButton(String text, String bgColor) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:" + bgColor + "; -fx-text-fill:" + TEXT_WHITE + ";" +
                "-fx-font-family:Monospace; -fx-font-weight:bold; -fx-font-size:12;" +
                "-fx-padding:10 20; -fx-border-radius:4; -fx-background-radius:4; -fx-cursor:hand;");
        return btn;
    }

    private Region makeDivider() {
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setStyle("-fx-background-color:" + BORDER + ";");
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }
}