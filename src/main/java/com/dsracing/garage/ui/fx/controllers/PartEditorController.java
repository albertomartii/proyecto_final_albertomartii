package com.dsracing.garage.ui.fx.controllers;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.model.entity.PartType;
import com.dsracing.garage.service.CarService;
import com.dsracing.garage.service.impl.DynoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

/**
 * Pantalla de cambio de piezas.
 *
 * Layout:
 *  ┌─────────────────────────────────────────────────────┐
 *  │  NISSAN S13 · EDITOR DE PIEZAS                      │
 *  ├──────────────────────┬──────────────────────────────┤
 *  │  SLOTS (izquierda)   │  CATÁLOGO (derecha)          │
 *  │  • Turbo      [x]    │  [Tarjeta pieza disponible]  │
 *  │  • Escape     [ ]    │  [Tarjeta pieza disponible]  │
 *  │  • ECU        [x]    │  ...                         │
 *  ├──────────────────────┴──────────────────────────────┤
 *  │  [CANCELAR]                    [GUARDAR Y CERRAR]   │
 *  └─────────────────────────────────────────────────────┘
 */
public class PartEditorController {

    // ── Colores ──────────────────────────────────────────────────────────────
    private static final String BG_DARK    = "#0d0d0f";
    private static final String BG_PANEL   = "#13131a";
    private static final String BG_SLOT    = "#0f0f18";
    private static final String ACCENT_RED = "#e8002d";
    private static final String ACCENT_YEL = "#f5c400";
    private static final String ACCENT_GRN = "#00e676";
    private static final String TEXT_WHITE = "#f0f0f0";
    private static final String TEXT_GRAY  = "#666680";
    private static final String BORDER     = "#2a2a3a";
    private static final String BORDER_ACT = "#e8002d";

    // ── Catálogo de piezas disponibles (hardcoded para el MVP) ───────────────
    // Cada array: {name, type, hpDelta, torqueDelta, weightDelta, gripDelta, suspDelta, desc}
    private static final String[][] PARTS_CATALOG = {
            // TURBO
            {"Turbo Stage 1",   "TURBO",      "40",  "55",  "8",  "0",    "0",    "Turbo de entrada. Mejora potencia a altas RPM."},
            {"Turbo Stage 2",   "TURBO",      "70",  "90",  "12", "0",    "0",    "Turbo competición. Gran ganancia de potencia."},
            {"Turbo Stage 3",   "TURBO",      "110", "130", "15", "0",    "0",    "Turbo de rally. Máxima potencia, menos respuesta."},
            // ESCAPE
            {"Escape Sport",    "EXHAUST",    "15",  "10",  "-5", "0",    "0",    "Escape deportivo. Ligera mejora y reducción de peso."},
            {"Escape Racing",   "EXHAUST",    "28",  "18",  "-9", "0",    "0",    "Escape de competición. Titanio, muy ligero."},
            // ECU
            {"ECU Stage 1",     "INTAKE",     "20",  "15",  "0",  "0",    "0",    "Reprogramación básica de la centralita."},
            {"ECU Stage 2",     "INTAKE",     "35",  "25",  "0",  "0",    "0",    "Mapa de competición. Optimiza todo el rango de RPM."},
            // SUSPENSIÓN
            {"Suspensión Sport","SUSPENSION", "0",   "0",   "-20","0.08", "15",   "Muelles y amortiguadores deportivos."},
            {"Suspensión Rally", "SUSPENSION","0",   "0",   "-15","0.05", "8",    "Configuración rally. Mayor recorrido."},
            {"Suspensión Drift", "SUSPENSION","0",   "0",   "-18","0.12", "22",   "Configuración drift. Máximo ángulo."},
            // NEUMÁTICOS
            {"Neumáticos Sport", "TIRES",     "0",   "0",   "0",  "0.10", "0",    "Neumáticos semi-slick. Gran agarre en seco."},
            {"Neumáticos Drift", "TIRES",     "0",   "0",   "0",  "0.14", "0",    "Compuesto blando. Ideal para drifting."},
            {"Neumáticos Rally", "TIRES",     "0",   "0",   "5",  "0.07", "0",    "Neumáticos mixtos todo-terreno."},
            // DIFERENCIAL
            {"Dif. Deportivo",  "DIFF",       "0",   "0",   "-8", "0.06", "0",    "Diferencial de deslizamiento limitado básico."},
            {"Dif. Racing",     "DIFF",       "0",   "0",   "-12","0.10", "0",    "Diferencial de competición. Control total."},
            // FRENOS
            {"Frenos Sport",    "BRAKES",     "0",   "0",   "-10","0.05", "0",    "Pinzas de 4 pistones y discos ventilados."},
            {"Frenos Racing",   "BRAKES",     "0",   "0",   "-18","0.08", "0",    "Kit de frenos de competición. Carbono-cerámico."},
            // AERO
            {"Alerón Trasero",  "AERO",       "0",   "0",   "8",  "0.06", "0",    "Alerón fijo. Aumenta la carga aerodinámica."},
            {"Kit Aero Completo","AERO",      "0",   "0",   "15", "0.12", "0",    "Splitter + difusor + alerón. Máxima carga."},
    };

    // ── Estado ────────────────────────────────────────────────────────────────
    private Car        car;
    private CarService carService;
    private DynoService dynoService;
    private Consumer<Car> onSaved; // callback al guardar

    // Mapa slot → pieza seleccionada actualmente (null = vacío)
    private final Map<PartType, Part> equippedSlots = new LinkedHashMap<>();

    // Slot seleccionado actualmente en el panel izquierdo
    private PartType selectedSlot = null;

    // Referencias UI que se actualizan dinámicamente
    private VBox    slotsPanel;
    private VBox    catalogPanel;
    private Label   previewStatsLabel;

    // ════════════════════════════════════════════════════════════════════════
    //  Entrada pública
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @param car        Coche a editar
     * @param carService Para guardar los cambios
     * @param dynoService Para preview de curva (futuro)
     * @param onSaved   Callback que recibe el Car actualizado tras guardar
     */
    public void show(Car car, CarService carService, DynoService dynoService,
                     Consumer<Car> onSaved) {
        this.car        = car;
        this.carService = carService;
        this.dynoService = dynoService;
        this.onSaved    = onSaved;

        // Inicializar slots con las piezas actuales del coche
        for (PartType type : PartType.values()) {
            equippedSlots.put(type, null);
        }
        if (car.getParts() != null) {
            for (Part p : car.getParts()) {
                equippedSlots.put(p.getType(), p);
            }
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("DS Racing · Piezas — " + car.getMake() + " " + car.getModel());
        stage.setScene(buildScene());
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(580);
        stage.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construcción de la escena
    // ════════════════════════════════════════════════════════════════════════

    private Scene buildScene() {
        // ── Título ───────────────────────────────────────────────────────
        Label tag = new Label("EDITOR DE PIEZAS");
        tag.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        tag.setTextFill(Color.web(ACCENT_RED));

        Label carName = new Label(
                car.getMake().toUpperCase() + "  " + car.getModel().toUpperCase()
                        + "  ·  " + car.getYear());
        carName.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        carName.setTextFill(Color.web(TEXT_WHITE));

        // Stats base del coche
        previewStatsLabel = new Label(buildStatsText());
        previewStatsLabel.setFont(Font.font("Monospace", 11));
        previewStatsLabel.setTextFill(Color.web(TEXT_GRAY));

        VBox header = new VBox(4, tag, carName, previewStatsLabel);
        header.setPadding(new Insets(0, 0, 16, 0));

        // ── Panel izquierdo: slots ────────────────────────────────────────
        slotsPanel = new VBox(8);
        slotsPanel.setPrefWidth(300);
        slotsPanel.setMinWidth(260);
        rebuildSlotsPanel();

        Label slotsTitle = makeSection("PIEZAS INSTALADAS");
        VBox leftBox = new VBox(10, slotsTitle, slotsPanel);
        leftBox.setPadding(new Insets(0, 16, 0, 0));
        leftBox.setPrefWidth(300);

        // ── Panel derecho: catálogo ───────────────────────────────────────
        catalogPanel = new VBox(8);
        rebuildCatalogPanel(null); // muestra todo inicialmente

        Label catalogTitle = makeSection("CATÁLOGO DE PIEZAS");
        ScrollPane catalogScroll = new ScrollPane(catalogPanel);
        catalogScroll.setFitToWidth(true);
        catalogScroll.setStyle("-fx-background-color:" + BG_DARK +
                "; -fx-background:" + BG_DARK + ";");

        VBox rightBox = new VBox(10, catalogTitle, catalogScroll);
        VBox.setVgrow(catalogScroll, Priority.ALWAYS);

        // ── Split layout ──────────────────────────────────────────────────
        HBox centerBox = new HBox(0, leftBox, makeDividerV(), rightBox);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        centerBox.setPadding(new Insets(0));

        // ── Barra inferior ────────────────────────────────────────────────
        Button btnCancel = makeBtn("CANCELAR", BG_PANEL, TEXT_GRAY, BORDER);
        Button btnSave   = makeBtn("✔  GUARDAR Y CERRAR", "#1a2a1a", ACCENT_GRN, ACCENT_GRN);

        btnCancel.setOnAction(e ->
                ((Stage) btnCancel.getScene().getWindow()).close());

        btnSave.setOnAction(e -> saveAndClose(
                (Stage) btnSave.getScene().getWindow()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomBar = new HBox(12, btnCancel, spacer, btnSave);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        // ── Root ──────────────────────────────────────────────────────────
        VBox root = new VBox(0, header, makeDividerH(), centerBox,
                makeDividerH(), bottomBar);
        VBox.setVgrow(centerBox, Priority.ALWAYS);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:" + BG_DARK + ";");

        return new Scene(root, 960, 620);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel de slots (izquierda)
    // ════════════════════════════════════════════════════════════════════════

    private void rebuildSlotsPanel() {
        slotsPanel.getChildren().clear();

        for (Map.Entry<PartType, Part> entry : equippedSlots.entrySet()) {
            PartType type  = entry.getKey();
            Part     part  = entry.getValue();
            boolean  active = type == selectedSlot;

            // ── Fila del slot ─────────────────────────────────────────────
            Label typeLbl = new Label(typeLabel(type));
            typeLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
            typeLbl.setTextFill(Color.web(TEXT_GRAY));
            typeLbl.setMinWidth(90);

            Label partLbl;
            if (part != null) {
                partLbl = new Label(part.getName());
                partLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
                partLbl.setTextFill(Color.web(ACCENT_GRN));
            } else {
                partLbl = new Label("— vacío —");
                partLbl.setFont(Font.font("Monospace", 12));
                partLbl.setTextFill(Color.web(TEXT_GRAY));
            }

            // Botón quitar (solo si hay pieza)
            Button btnRemove = new Button("✕");
            btnRemove.setVisible(part != null);
            btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: " +
                    ACCENT_RED + "; -fx-font-size: 11; -fx-cursor: hand; -fx-padding: 0 4;");
            btnRemove.setOnAction(e -> {
                equippedSlots.put(type, null);
                updatePreviewStats();
                rebuildSlotsPanel();
                rebuildCatalogPanel(selectedSlot);
            });

            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox row = new HBox(8, typeLbl, partLbl, gap, btnRemove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));

            String borderColor = active ? ACCENT_RED : BORDER;
            String bgColor     = active ? "#1a0010"  : BG_SLOT;
            row.setStyle("-fx-background-color:" + bgColor + ";" +
                    "-fx-border-color:" + borderColor + "; -fx-border-width:1;" +
                    "-fx-border-radius:5; -fx-background-radius:5; -fx-cursor:hand;");

            // Click en slot → filtra el catálogo
            row.setOnMouseClicked(e -> {
                selectedSlot = (selectedSlot == type) ? null : type;
                rebuildSlotsPanel();
                rebuildCatalogPanel(selectedSlot);
            });

            slotsPanel.getChildren().add(row);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Panel de catálogo (derecha)
    // ════════════════════════════════════════════════════════════════════════

    private void rebuildCatalogPanel(PartType filterType) {
        catalogPanel.getChildren().clear();

        if (filterType != null) {
            Label filter = new Label("Mostrando: " + typeLabel(filterType) +
                    "  ·  click en slot para ver todo");
            filter.setFont(Font.font("Monospace", 10));
            filter.setTextFill(Color.web(ACCENT_YEL));
            catalogPanel.getChildren().add(filter);
        }

        for (String[] data : PARTS_CATALOG) {
            PartType type = PartType.valueOf(data[1]);

            // Filtrar si hay un slot seleccionado
            if (filterType != null && type != filterType) continue;

            Part part = toPart(data);
            boolean isEquipped = equippedSlots.get(type) != null &&
                    equippedSlots.get(type).getName().equals(part.getName());

            catalogPanel.getChildren().add(buildPartCard(part, type, isEquipped, data[7]));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tarjeta de pieza en el catálogo
    // ════════════════════════════════════════════════════════════════════════

    private HBox buildPartCard(Part part, PartType type,
                               boolean isEquipped, String desc) {
        // ── Badge de tipo ─────────────────────────────────────────────────
        Label typeBadge = new Label(typeLabel(type));
        typeBadge.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        typeBadge.setTextFill(Color.web(typeColor(type)));
        typeBadge.setStyle("-fx-border-color:" + typeColor(type) + ";" +
                "-fx-border-width:1; -fx-border-radius:3; -fx-padding: 1 5;");

        // ── Nombre ────────────────────────────────────────────────────────
        Label nameLbl = new Label(part.getName());
        nameLbl.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        nameLbl.setTextFill(Color.web(isEquipped ? ACCENT_GRN : TEXT_WHITE));

        // ── Descripción ───────────────────────────────────────────────────
        Label descLbl = new Label(desc);
        descLbl.setFont(Font.font("Monospace", 10));
        descLbl.setTextFill(Color.web(TEXT_GRAY));
        descLbl.setWrapText(true);

        // ── Deltas de stats ───────────────────────────────────────────────
        HBox deltas = buildDeltaRow(part);

        VBox info = new VBox(4, new HBox(8, typeBadge), nameLbl, descLbl, deltas);
        info.setAlignment(Pos.TOP_LEFT);

        // ── Botón instalar / instalada ────────────────────────────────────
        Button btnInstall;
        if (isEquipped) {
            btnInstall = makeBtn("✔ INSTALADA", "#0d1a0d", ACCENT_GRN, ACCENT_GRN);
            btnInstall.setDisable(true);
        } else {
            btnInstall = makeBtn("INSTALAR", BG_PANEL, TEXT_WHITE, ACCENT_RED);
            btnInstall.setOnAction(e -> {
                equippedSlots.put(type, part);
                selectedSlot = type;
                updatePreviewStats();
                rebuildSlotsPanel();
                rebuildCatalogPanel(selectedSlot);
            });
        }
        btnInstall.setMaxWidth(120);
        btnInstall.setMinWidth(100);

        HBox card = new HBox(12, info, new Region(), btnInstall);
        HBox.setHgrow(info, Priority.ALWAYS);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));

        String bg     = isEquipped ? "#0d180d" : BG_PANEL;
        String border = isEquipped ? ACCENT_GRN : BORDER;
        card.setStyle("-fx-background-color:" + bg + ";" +
                "-fx-border-color:" + border + "; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        // Hover
        card.setOnMouseEntered(e -> {
            if (!isEquipped) card.setStyle("-fx-background-color:#1a1a25;" +
                    "-fx-border-color:" + ACCENT_RED + "; -fx-border-width:1;" +
                    "-fx-border-radius:6; -fx-background-radius:6;");
        });
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:" + bg + ";" +
                "-fx-border-color:" + border + "; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;"));

        return card;
    }

    // ── Fila de deltas de estadísticas ────────────────────────────────────────
    private HBox buildDeltaRow(Part part) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 0, 0));

        if (part.getHpDelta()    != 0) row.getChildren().add(delta("HP",  part.getHpDelta()));
        if (part.getTorqueDelta() != 0) row.getChildren().add(delta("Nm",  part.getTorqueDelta()));
        if (part.getWeightDelta() != 0) row.getChildren().add(delta("kg",  part.getWeightDelta()));
        if (part.getGripDelta()  != 0) row.getChildren().add(delta("GRP", part.getGripDelta() * 100));
        if (part.getSuspensionStiffnessDelta() != 0)
            row.getChildren().add(delta("SUS", part.getSuspensionStiffnessDelta()));

        return row;
    }

    private VBox delta(String unit, double value) {
        boolean positive = (unit.equals("kg")) ? value < 0 : value > 0;
        String color = positive ? ACCENT_GRN : ACCENT_RED;
        String sign  = value > 0 ? "+" : "";

        Label val = new Label(sign + String.format("%.0f", value));
        val.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        val.setTextFill(Color.web(color));

        Label lbl = new Label(unit);
        lbl.setFont(Font.font("Monospace", 9));
        lbl.setTextFill(Color.web(TEXT_GRAY));

        VBox box = new VBox(0, val, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Guardar
    // ════════════════════════════════════════════════════════════════════════

    private void saveAndClose(Stage stage) {
        // Recopilar piezas equipadas
        List<Part> parts = new ArrayList<>();
        for (Part p : equippedSlots.values()) {
            if (p != null) parts.add(p);
        }
        car.setParts(parts);
        Car saved = carService.save(car);

        if (onSaved != null) onSaved.accept(saved);
        stage.close();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Preview de stats con piezas equipadas
    // ════════════════════════════════════════════════════════════════════════

    private void updatePreviewStats() {
        if (previewStatsLabel != null) {
            previewStatsLabel.setText(buildStatsText());
        }
    }

    private String buildStatsText() {
        double hp     = car.getBasePower();
        double torque = car.getBaseTorque();
        double mass   = car.getMass();
        double grip   = car.getGripBase();

        for (Part p : equippedSlots.values()) {
            if (p == null) continue;
            hp     += p.getHpDelta();
            torque += p.getTorqueDelta();
            mass   += p.getWeightDelta();
            grip   += p.getGripDelta();
        }

        return String.format(
                "Base: %.0f HP  %.0f Nm  %.0f kg  ·  Con piezas: %.0f HP  %.0f Nm  %.0f kg  grip %.2f",
                car.getBasePower(), car.getBaseTorque(), car.getMass(),
                hp, torque, mass, grip);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Utilidades
    // ════════════════════════════════════════════════════════════════════════

    /** Convierte una fila del catálogo en un objeto Part transitorio. */
    private Part toPart(String[] data) {
        Part p = new Part();
        p.setName(data[0]);
        p.setType(PartType.valueOf(data[1]));
        p.setHpDelta(Double.parseDouble(data[2]));
        p.setTorqueDelta(Double.parseDouble(data[3]));
        p.setWeightDelta(Double.parseDouble(data[4]));
        p.setGripDelta(Double.parseDouble(data[5]));
        p.setSuspensionStiffnessDelta(Double.parseDouble(data[6]));
        return p;
    }

    private String typeLabel(PartType type) {
        return switch (type) {
            case TURBO      -> "TURBO";
            case EXHAUST    -> "ESCAPE";
            case INTAKE     -> "ECU / ADMISIÓN";
            case SUSPENSION -> "SUSPENSIÓN";
            case TIRES      -> "NEUMÁTICOS";
            case DIFF       -> "DIFERENCIAL";
            case BRAKES     -> "FRENOS";
            case AERO       -> "AERODINÁMICA";
        };
    }

    private String typeColor(PartType type) {
        return switch (type) {
            case TURBO      -> "#e8002d";
            case EXHAUST    -> "#ff6d00";
            case INTAKE     -> "#f5c400";
            case SUSPENSION -> "#00c8ff";
            case TIRES      -> "#00e676";
            case DIFF       -> "#aa00ff";
            case BRAKES     -> "#ff4081";
            case AERO       -> "#40c4ff";
        };
    }

    private Button makeBtn(String text, String bg, String fg, String border) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                "-fx-border-color:" + border + "; -fx-border-width:1;" +
                "-fx-font-family:Monospace; -fx-font-weight:bold; -fx-font-size:11;" +
                "-fx-padding:9 18; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-cursor:hand;");
        return btn;
    }

    private Label makeSection(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(TEXT_GRAY));
        l.setPadding(new Insets(0, 0, 4, 0));
        return l;
    }

    private Region makeDividerH() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:" + BORDER + ";");
        VBox.setMargin(r, new Insets(8, 0, 8, 0));
        return r;
    }

    private Region makeDividerV() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setMaxHeight(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:" + BORDER + ";");
        HBox.setMargin(r, new Insets(0, 16, 0, 0));
        return r;
    }
}