package com.dsracing.garage.ui.fx.controllers;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.service.impl.DynoService;
import com.dsracing.garage.util.csv.CsvExporter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static javax.swing.text.html.CSS.Attribute.BORDER;

/**
 * Ventana de dinamómetro con:
 *  - Gráfico animado de curva de potencia (HP y Torque) que se dibuja en tiempo real
 *  - Cuenta-RPM: sube de RPM_MIN a RPM_MAX durante la prueba, luego baja a 0 (corte)
 *  - Indicadores numéricos de HP y Nm actuales
 *  - Botón para exportar CSV al finalizar
 */
public class DynoController {

    // ── Parámetros de la prueba ──────────────────────────────────────────────
    private static final int RPM_MIN  = 1000;
    private static final int RPM_MAX  = 8000;
    private static final int RPM_STEP = 250;

    // Velocidad de la animación: ms entre cada punto de RPM durante la subida
    private static final int MS_PER_RPM_STEP = 80;
    // Velocidad de la caída de RPM al finalizar (ms por tick de bajada)
    private static final int MS_PER_DROP_TICK = 30;
    // Cuántos RPM baja por tick al cortar
    private static final int RPM_DROP_PER_TICK = 300;

    // ── UI ──────────────────────────────────────────────────────────────────
    private Stage  stage;
    private Canvas chartCanvas;
    private Canvas gaugeCanvas;
    private Label  labelHP;
    private Label  labelNm;
    private Label  labelRPM;
    private Label  labelStatus;
    private Button btnExport;

    // ── Datos de la prueba ───────────────────────────────────────────────────
    private Car         car;
    private DynoResult  dynoResult;
    private DynoService dynoService;

    // Puntos ya revelados durante la animación (para dibujar la curva creciente)
    private final List<Integer> revealedRpm    = new ArrayList<>();
    private final List<Double>  revealedPower  = new ArrayList<>();
    private final List<Double>  revealedTorque = new ArrayList<>();

    // Curvas completas calculadas antes de animar
    private Map<Integer, Double> fullPowerCurve  = new TreeMap<>();
    private Map<Integer, Double> fullTorqueCurve = new TreeMap<>();

    private double maxPowerDisplay  = 1;
    private double maxTorqueDisplay = 1;

    // RPM actual del gauge (se usa también en la fase de bajada)
    private int currentGaugeRpm = 0;

    // ── Colores ──────────────────────────────────────────────────────────────
    private static final Color BG_DARK    = Color.web("#0d0d0f");
    private static final Color BG_PANEL   = Color.web("#13131a");
    private static final Color ACCENT_RED = Color.web("#e8002d");
    private static final Color ACCENT_YEL = Color.web("#f5c400");
    private static final Color ACCENT_CYN = Color.web("#00c8ff");
    private static final Color TEXT_WHITE = Color.web("#f0f0f0");
    private static final Color TEXT_GRAY  = Color.web("#666680");
    private static final Color GRID_COLOR = Color.web("#1e1e2e");

    // ════════════════════════════════════════════════════════════════════════
    //  Entrada pública
    // ════════════════════════════════════════════════════════════════════════

    public void show(Car car, DynoService dynoService) {
        this.car         = car;
        this.dynoService = dynoService;

        buildFullCurves();

        Platform.runLater(() -> {
            stage = new Stage();
            stage.setTitle("DS Racing · Dinamómetro — " + car.getMake() + " " + car.getModel());
            stage.setScene(buildScene());
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setWidth(960);
            stage.setHeight(620);
            stage.centerOnScreen();
            stage.show();
            stage.sizeToScene();      // ← añadir
            startDynoAnimation();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construcción de la escena
    // ════════════════════════════════════════════════════════════════════════

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0d0f;");
        root.setPadding(new Insets(20));

        // Título
        Label tag = new Label("DINAMÓMETRO");
        tag.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        tag.setTextFill(ACCENT_RED);

        Label carLabel = new Label(
                car.getMake().toUpperCase() + "  " + car.getModel().toUpperCase()
                        + "  ·  " + car.getYear());
        carLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        carLabel.setTextFill(TEXT_WHITE);

        VBox titleBox = new VBox(4, tag, carLabel);
        titleBox.setPadding(new Insets(0, 0, 16, 0));

        // Canvas del gráfico
        chartCanvas = new Canvas(700, 300);
        drawChartBackground();

        // Gauge
        gaugeCanvas = new Canvas(220, 220);
        drawGauge(0);

        labelRPM = makeValueLabel("0", 28, ACCENT_YEL);
        Label rpmUnit = makeUnitLabel("RPM");

        labelHP = makeValueLabel("0", 36, ACCENT_RED);
        Label hpUnit = makeUnitLabel("HP");

        labelNm = makeValueLabel("0", 36, ACCENT_CYN);
        Label nmUnit = makeUnitLabel("Nm");

        VBox indicators = new VBox(10,
                centered(gaugeCanvas),
                centered(labelRPM), centered(rpmUnit),
                separator(),
                row(labelHP, hpUnit),
                row(labelNm, nmUnit)
        );
        indicators.setPadding(new Insets(0, 0, 0, 20));
        indicators.setAlignment(Pos.TOP_CENTER);
        indicators.setMinWidth(220);

        // Status y botón export
        labelStatus = new Label("Iniciando prueba...");
        labelStatus.setFont(Font.font("Monospace", 12));
        labelStatus.setTextFill(TEXT_GRAY);

        btnExport = new Button("⬇  EXPORTAR CSV");
        btnExport.setStyle(
                "-fx-background-color: #1a1a2a; -fx-text-fill: #666680;" +
                        "-fx-border-color: #333355; -fx-border-width: 1;" +
                        "-fx-font-family: Monospace; -fx-font-size: 12; -fx-padding: 8 20;");
        btnExport.setDisable(true);
        btnExport.setOnAction(e -> exportCsv());

        HBox bottomBar = new HBox(20, labelStatus, btnExport);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(14, 0, 0, 0));

        HBox centerBox = new HBox(10, chartCanvas, indicators);
        centerBox.setAlignment(Pos.TOP_LEFT);

        VBox mainBox = new VBox(0, titleBox, centerBox, bottomBar);
        root.setCenter(mainBox);

        return new Scene(root, 980, 550);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Animación principal
    // ════════════════════════════════════════════════════════════════════════

    private void startDynoAnimation() {
        labelStatus.setText("▶  Acelerando...");
        labelStatus.setTextFill(ACCENT_YEL);

        List<Integer> rpmList = new ArrayList<>(fullPowerCurve.keySet());

        // ── FASE 1: Subida de RPM dibujando la curva ─────────────────────
        Timeline riseTimeline = new Timeline();
        riseTimeline.setCycleCount(1);

        for (int i = 0; i < rpmList.size(); i++) {
            final int idx = i;
            riseTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis((long) MS_PER_RPM_STEP * i), e -> {
                        int    rpm    = rpmList.get(idx);
                        double power  = fullPowerCurve.getOrDefault(rpm, 0.0);
                        double torque = fullTorqueCurve.getOrDefault(rpm, 0.0);

                        currentGaugeRpm = rpm;
                        revealedRpm.add(rpm);
                        revealedPower.add(power);
                        revealedTorque.add(torque);

                        labelRPM.setText(String.valueOf(rpm));
                        labelHP.setText(String.format("%.0f", power));
                        labelNm.setText(String.format("%.0f", torque));

                        drawChartBackground();
                        drawCurves();
                        drawGauge(rpm);
                    })
            );
        }

        // ── FASE 2: Corte — el RPM baja a 0 rápidamente ──────────────────
        riseTimeline.setOnFinished(e -> {
            // Calcular resultado final (usando el servicio)
            dynoResult = dynoService.runDyno(car, car.getParts());

            labelStatus.setText("■  Corte — bajando RPM...");
            labelStatus.setTextFill(TEXT_GRAY);

            // Número de ticks para bajar de RPM_MAX a 0
            int ticks = (RPM_MAX / RPM_DROP_PER_TICK) + 1;

            Timeline dropTimeline = new Timeline();
            dropTimeline.setCycleCount(1);

            for (int t = 0; t <= ticks; t++) {
                final int tick = t;
                dropTimeline.getKeyFrames().add(
                        new KeyFrame(Duration.millis((long) MS_PER_DROP_TICK * t), ev -> {
                            int dropRpm = Math.max(0, RPM_MAX - tick * RPM_DROP_PER_TICK);
                            currentGaugeRpm = dropRpm;

                            labelRPM.setText(String.valueOf(dropRpm));
                            // HP y Nm van a 0 junto con el RPM
                            double pct = (double) dropRpm / RPM_MAX;
                            labelHP.setText(String.format("%.0f",
                                    dynoResult.getMaxPower() * pct));
                            labelNm.setText(String.format("%.0f",
                                    dynoResult.getMaxTorque() * pct));

                            drawGauge(dropRpm);
                            // La curva del gráfico queda dibujada completa
                            drawChartBackground();
                            drawCurves();
                        })
                );
            }

            dropTimeline.setOnFinished(ev -> onDynoFullyFinished());
            dropTimeline.play();
        });

        riseTimeline.play();
    }

    private void onDynoFullyFinished() {
        labelRPM.setText("0");
        labelHP.setText(String.format("%.0f", dynoResult.getMaxPower()));
        labelNm.setText(String.format("%.0f", dynoResult.getMaxTorque()));
        drawGauge(0);

        labelStatus.setText("✔  Prueba completada — Pico: " +
                String.format("%.0f", dynoResult.getMaxPower()) + " HP  /  " +
                String.format("%.0f", dynoResult.getMaxTorque()) + " Nm");
        labelStatus.setTextFill(Color.web("#00e676"));

        // Activar botón de exportar
        btnExport.setDisable(false);
        btnExport.setStyle(
                "-fx-background-color: #1a2a1a; -fx-text-fill: #00e676;" +
                        "-fx-border-color: #00e676; -fx-border-width: 1;" +
                        "-fx-font-family: Monospace; -fx-font-size: 12; -fx-padding: 8 20;" +
                        "-fx-cursor: hand;");

        // Redibujar el gráfico mostrando los valores pico con anotaciones
        drawChartBackground();
        drawCurves();
        drawPeakAnnotations();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Dibujo del gráfico
    // ════════════════════════════════════════════════════════════════════════

    private static final double PAD_L = 60;
    private static final double PAD_R = 20;
    private static final double PAD_T = 20;
    private static final double PAD_B = 40;

    private void drawChartBackground() {
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        double w  = chartCanvas.getWidth();
        double h  = chartCanvas.getHeight();
        double cw = w - PAD_L - PAD_R;
        double ch = h - PAD_T - PAD_B;

        // Fondo
        gc.setFill(BG_DARK);
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.web("#101018"));
        gc.fillRoundRect(PAD_L, PAD_T, cw, ch, 6, 6);

        // Grid horizontal con etiquetas HP y Nm en dos ejes
        int gridLines = 6;
        for (int i = 0; i <= gridLines; i++) {
            double y = PAD_T + ch * i / gridLines;
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(1);
            gc.strokeLine(PAD_L, y, PAD_L + cw, y);

            // Eje izquierdo: HP
            double valHP = maxPowerDisplay * (1.0 - (double) i / gridLines);
            gc.setFill(Color.web("#cc3344"));
            gc.setFont(Font.font("Monospace", 9));
            gc.fillText(String.format("%.0f", valHP), 4, y + 4);

            // Eje derecho: Nm
            double valNm = maxTorqueDisplay * (1.0 - (double) i / gridLines);
            gc.setFill(Color.web("#0099bb"));
            gc.setFont(Font.font("Monospace", 9));
            gc.fillText(String.format("%.0f", valNm), PAD_L + cw + 4, y + 4);
        }

        // Grid vertical (RPM marks)
        int[] rpmMarks = {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000};
        for (int rpm : rpmMarks) {
            double x = rpmToX(rpm, cw);
            gc.setStroke(GRID_COLOR);
            gc.setLineWidth(1);
            gc.strokeLine(x, PAD_T, x, PAD_T + ch);
            gc.setFill(TEXT_GRAY);
            gc.setFont(Font.font("Monospace", 10));
            gc.fillText(rpm / 1000 + "k", x - 8, PAD_T + ch + 16);
        }

        // Etiquetas de ejes
        gc.setFill(ACCENT_RED);
        gc.setFont(Font.font("Monospace", 10));
        gc.fillText("HP", 6, PAD_T - 6);

        gc.setFill(ACCENT_CYN);
        gc.fillText("Nm", PAD_L + cw + 4, PAD_T - 6);

        gc.setFill(TEXT_GRAY);
        gc.setFont(Font.font("Monospace", 11));
        gc.fillText("RPM", PAD_L + cw / 2 - 12, h - 4);

        // Leyenda
        gc.setFill(ACCENT_RED);
        gc.fillRect(PAD_L + 10, PAD_T + 8, 18, 3);
        gc.setFill(TEXT_WHITE);
        gc.setFont(Font.font("Monospace", 10));
        gc.fillText("Potencia (HP)", PAD_L + 34, PAD_T + 13);

        gc.setFill(ACCENT_CYN);
        gc.fillRect(PAD_L + 145, PAD_T + 8, 18, 3);
        gc.setFill(TEXT_WHITE);
        gc.fillText("Torque (Nm)", PAD_L + 169, PAD_T + 13);
    }

    private void drawCurves() {
        if (revealedRpm.isEmpty()) return;
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        double cw = chartCanvas.getWidth()  - PAD_L - PAD_R;
        double ch = chartCanvas.getHeight() - PAD_T - PAD_B;

        // Curva de potencia (rojo)
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < revealedRpm.size(); i++) {
            double x = rpmToX(revealedRpm.get(i), cw);
            double y = valToY(revealedPower.get(i), maxPowerDisplay, ch);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        // Punto en el extremo
        int last = revealedRpm.size() - 1;
        double lx = rpmToX(revealedRpm.get(last), cw);
        double ly = valToY(revealedPower.get(last), maxPowerDisplay, ch);
        gc.setFill(ACCENT_RED);
        gc.fillOval(lx - 4, ly - 4, 8, 8);

        // Curva de torque (cian) — usa su propio eje (maxTorqueDisplay)
        gc.setStroke(ACCENT_CYN);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < revealedRpm.size(); i++) {
            double x = rpmToX(revealedRpm.get(i), cw);
            double y = valToY(revealedTorque.get(i), maxTorqueDisplay, ch);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        gc.setFill(ACCENT_CYN);
        double tx = rpmToX(revealedRpm.get(last), cw);
        double ty = valToY(revealedTorque.get(last), maxTorqueDisplay, ch);
        gc.fillOval(tx - 4, ty - 4, 8, 8);
    }

    /** Dibuja etiquetas de pico de HP y Nm al finalizar */
    private void drawPeakAnnotations() {
        if (dynoResult == null) return;
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        double cw = chartCanvas.getWidth()  - PAD_L - PAD_R;
        double ch = chartCanvas.getHeight() - PAD_T - PAD_B;

        // Buscar RPM del pico de potencia
        int peakPowerRpm = fullPowerCurve.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(5000);

        double px = rpmToX(peakPowerRpm, cw);
        double py = valToY(fullPowerCurve.get(peakPowerRpm), maxPowerDisplay, ch);

        // Línea vertical pico HP
        gc.setStroke(Color.web("#e8002d55"));
        gc.setLineWidth(1);
        gc.strokeLine(px, PAD_T, px, PAD_T + ch);

        // Etiqueta HP máximo
        gc.setFill(ACCENT_RED);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        gc.fillText(String.format("%.0f HP", dynoResult.getMaxPower()), px + 4, py - 6);

        // Buscar RPM del pico de torque
        int peakTorqueRpm = fullTorqueCurve.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(4000);

        double tx = rpmToX(peakTorqueRpm, cw);
        double ty = valToY(fullTorqueCurve.get(peakTorqueRpm), maxTorqueDisplay, ch);

        gc.setFill(ACCENT_CYN);
        gc.fillText(String.format("%.0f Nm", dynoResult.getMaxTorque()), tx + 4, ty - 6);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Gauge (cuenta-RPM)
    // ════════════════════════════════════════════════════════════════════════

    private void drawGauge(int rpm) {
        GraphicsContext gc = gaugeCanvas.getGraphicsContext2D();
        double w  = gaugeCanvas.getWidth();
        double h  = gaugeCanvas.getHeight();
        double cx = w / 2;
        double cy = h / 2 + 10;
        double r  = Math.min(w, h) / 2 - 14;

        gc.clearRect(0, 0, w, h);
        gc.setFill(BG_PANEL);
        gc.fillOval(cx - r - 12, cy - r - 12, (r + 12) * 2, (r + 12) * 2);

        double startAngle = 220;
        double totalArc   = 280;

        // Arco de fondo (gris oscuro)
        gc.setStroke(Color.web("#1e1e2e"));
        gc.setLineWidth(10);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, -totalArc,
                javafx.scene.shape.ArcType.OPEN);

        // Arco de progreso coloreado por zonas
        double pct = Math.min(1.0, Math.max(0.0,
                (double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN)));

        if (pct > 0) {
            // Verde (0–60%)
            double arcGreen = totalArc * Math.min(pct, 0.60);
            gc.setStroke(Color.web("#00c853"));
            gc.setLineWidth(10);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                    startAngle, -arcGreen, javafx.scene.shape.ArcType.OPEN);

            // Amarillo (60–85%)
            if (pct > 0.60) {
                double arcYel = totalArc * (Math.min(pct, 0.85) - 0.60);
                gc.setStroke(ACCENT_YEL);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle - totalArc * 0.60, -arcYel,
                        javafx.scene.shape.ArcType.OPEN);
            }

            // Rojo (85–100%)
            if (pct > 0.85) {
                double arcRed = totalArc * (pct - 0.85);
                gc.setStroke(ACCENT_RED);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle - totalArc * 0.85, -arcRed,
                        javafx.scene.shape.ArcType.OPEN);
            }
        }

        // Marcas y etiquetas de RPM
        gc.setStroke(TEXT_GRAY);
        gc.setLineWidth(1.5);
        gc.setFill(TEXT_GRAY);
        gc.setFont(Font.font("Monospace", 9));
        for (int mark = RPM_MIN; mark <= RPM_MAX; mark += 1000) {
            double angle = Math.toRadians(startAngle - totalArc *
                    ((double)(mark - RPM_MIN) / (RPM_MAX - RPM_MIN)));
            double x1 = cx + (r - 8)  * Math.cos(angle);
            double y1 = cy - (r - 8)  * Math.sin(angle);
            double x2 = cx +  r       * Math.cos(angle);
            double y2 = cy -  r       * Math.sin(angle);
            gc.strokeLine(x1, y1, x2, y2);
            double xt = cx + (r - 22) * Math.cos(angle) - 6;
            double yt = cy - (r - 22) * Math.sin(angle) + 4;
            gc.fillText(mark / 1000 + "k", xt, yt);
        }

        // Aguja
        double needleAngle = Math.toRadians(startAngle - totalArc * pct);
        double nx = cx + (r - 18) * Math.cos(needleAngle);
        double ny = cy - (r - 18) * Math.sin(needleAngle);
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(2.5);
        gc.strokeLine(cx, cy, nx, ny);

        // Centro
        gc.setFill(Color.web("#222230"));
        gc.fillOval(cx - 7, cy - 7, 14, 14);
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - 7, cy - 7, 14, 14);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Cálculo de curvas (delega en el servicio)
    // ════════════════════════════════════════════════════════════════════════

    private void buildFullCurves() {
        List<Part> parts = car.getParts();
        fullPowerCurve  = dynoService.getPowerCurve(car, parts);
        fullTorqueCurve = dynoService.getTorqueCurve(car, parts);

        maxPowerDisplay  = fullPowerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(300) * 1.15;
        maxTorqueDisplay = fullTorqueCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(400) * 1.15;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de coordenadas
    // ════════════════════════════════════════════════════════════════════════

    private double rpmToX(int rpm, double chartWidth) {
        return PAD_L + chartWidth * ((double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN));
    }

    private double valToY(double val, double maxVal, double chartHeight) {
        return PAD_T + chartHeight * (1.0 - val / maxVal);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers de layout
    // ════════════════════════════════════════════════════════════════════════

    private Label makeValueLabel(String text, int size, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, size));
        l.setTextFill(color);
        return l;
    }

    private Label makeUnitLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Monospace", 11));
        l.setTextFill(TEXT_GRAY);
        return l;
    }

    private HBox row(Label value, Label unit) {
        HBox box = new HBox(6, value, unit);
        box.setAlignment(Pos.BASELINE_CENTER);
        return box;
    }

    private HBox centered(javafx.scene.Node node) {
        HBox box = new HBox(node);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private javafx.scene.shape.Line separator() {
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 180, 0);
        line.setStroke(Color.web("#1e1e2e"));
        return line;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Exportar CSV
    // ════════════════════════════════════════════════════════════════════════


    private void exportCsv() {
        if (dynoResult == null) return;
        try {
            // Pasamos las curvas completas para que el CSV incluya HP y Nm por RPM
            Path file = CsvExporter.exportDyno(
                    car,
                    dynoResult,
                    fullPowerCurve,   // Map<Integer,Double> ya calculado
                    fullTorqueCurve,  // Map<Integer,Double> ya calculado
                    Path.of("exports")
            );
            labelStatus.setText("✔  CSV exportado: " + file.getFileName());
            labelStatus.setTextFill(Color.web("#00e676"));
        } catch (Exception ex) {
            labelStatus.setText("✗  Error al exportar: " + ex.getMessage());
            labelStatus.setTextFill(ACCENT_RED);
        }
    }
}