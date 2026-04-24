package com.dsracing.garage.ui.fx.controllers;

import com.dsracing.garage.model.entity.Car;
import com.dsracing.garage.model.entity.DynoResult;
import com.dsracing.garage.model.entity.Part;
import com.dsracing.garage.service.impl.DynoService;
import com.dsracing.garage.util.csv.CsvExporter;
import javafx.animation.AnimationTimer;
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ventana de dinamómetro con:
 *  - Gráfico animado de curva de potencia (HP y Torque)
 *  - Velocímetro/cuenta-RPM con aguja animada
 *  - Indicadores numéricos de HP y Nm actuales
 *  - Botón para exportar CSV
 */
public class DynoController {

    // ── Parámetros de la prueba ──────────────────────────────────────────────
    private static final int RPM_MIN  = 1000;
    private static final int RPM_MAX  = 8000;
    private static final int RPM_STEP = 250;

    // ── UI ──────────────────────────────────────────────────────────────────
    private Stage stage;
    private Canvas chartCanvas;
    private Canvas gaugeCanvas;
    private Label  labelHP;
    private Label  labelNm;
    private Label  labelRPM;
    private Label  labelStatus;
    private Button btnExport;

    // ── Datos de la prueba ───────────────────────────────────────────────────
    private Car        car;
    private DynoResult dynoResult;
    private DynoService dynoService;

    // Puntos ya revelados durante la animación
    private final List<int[]>    rpmPoints    = new ArrayList<>(); // {rpm}
    private final List<double[]> powerPoints  = new ArrayList<>(); // {power}
    private final List<double[]> torquePoints = new ArrayList<>(); // {torque}

    // Curva completa calculada antes de animar
    private Map<Integer, Double> fullPowerCurve  = new TreeMap<>();
    private Map<Integer, Double> fullTorqueCurve = new TreeMap<>();

    private double maxPowerDisplay  = 1;
    private double maxTorqueDisplay = 1;

    private int    currentRPMIndex = 0;
    private boolean running        = false;
    private boolean finished       = false;

    // ── Colores estilo Racing ────────────────────────────────────────────────
    private static final Color BG_DARK    = Color.web("#0d0d0f");
    private static final Color BG_PANEL   = Color.web("#13131a");
    private static final Color ACCENT_RED = Color.web("#e8002d");
    private static final Color ACCENT_YEL = Color.web("#f5c400");
    private static final Color TEXT_WHITE = Color.web("#f0f0f0");
    private static final Color TEXT_GRAY  = Color.web("#666680");
    private static final Color GRID_COLOR = Color.web("#1e1e2e");

    // ════════════════════════════════════════════════════════════════════════
    //  Entrada pública
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Lanza la ventana del dyno.
     *
     * @param car        Coche a testear
     * @param dynoService Servicio para calcular la curva
     */
    public void show(Car car, DynoService dynoService) {
        this.car        = car;
        this.dynoService = dynoService;

        // Pre-calcular curvas completas
        buildFullCurves();

        Platform.runLater(() -> {
            stage = new Stage();
            stage.setTitle("DS Racing · Dinamómetro — " + car.getMake() + " " + car.getModel());
            stage.setScene(buildScene());
            stage.setResizable(false);
            stage.show();

            startDynoAnimation();
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construcción de la escena
    // ════════════════════════════════════════════════════════════════════════

    private Scene buildScene() {
        // ── Fondo principal ──────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d0d0f;");
        root.setPadding(new Insets(20));

        // ── Título ───────────────────────────────────────────────────────
        Label title = new Label("DINAMÓMETRO");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        title.setTextFill(ACCENT_RED);
        title.setStyle("-fx-letter-spacing: 4;");

        Label carLabel = new Label(car.getMake().toUpperCase() + "  " + car.getModel().toUpperCase()
                + "  ·  " + car.getYear());
        carLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        carLabel.setTextFill(TEXT_WHITE);

        VBox titleBox = new VBox(4, title, carLabel);
        titleBox.setPadding(new Insets(0, 0, 16, 0));

        // ── Gráfico de curva ─────────────────────────────────────────────
        chartCanvas = new Canvas(700, 300);
        drawChartBackground();

        // ── Panel derecho: gauge + indicadores ───────────────────────────
        gaugeCanvas = new Canvas(220, 220);
        drawGauge(0);

        labelRPM = makeValueLabel("0", 28, ACCENT_YEL);
        Label rpmUnit = makeUnitLabel("RPM");

        labelHP = makeValueLabel("0", 36, ACCENT_RED);
        Label hpUnit = makeUnitLabel("HP");

        labelNm = makeValueLabel("0", 36, Color.web("#00c8ff"));
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

        // ── Status + botón ───────────────────────────────────────────────
        labelStatus = new Label("Listo para iniciar prueba...");
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

        // ── Layout ───────────────────────────────────────────────────────
        HBox centerBox = new HBox(10, chartCanvas, indicators);
        centerBox.setAlignment(Pos.TOP_LEFT);

        VBox mainBox = new VBox(0, titleBox, centerBox, bottomBar);
        root.setCenter(mainBox);

        return new Scene(root, 980, 430);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Animación
    // ════════════════════════════════════════════════════════════════════════

    private void startDynoAnimation() {
        running = true;
        labelStatus.setText("▶  Prueba en curso...");
        labelStatus.setTextFill(ACCENT_YEL);

        List<Integer> rpmList = new ArrayList<>(fullPowerCurve.keySet());

        Timeline timeline = new Timeline();
        timeline.setCycleCount(1);

        // Un KeyFrame por cada punto de RPM (60ms entre puntos = ~1.7 seg total)
        for (int i = 0; i < rpmList.size(); i++) {
            final int idx = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(80L * i), e -> {
                int rpm = rpmList.get(idx);
                double power  = fullPowerCurve.get(rpm);
                double torque = fullTorqueCurve.getOrDefault(rpm, 0.0);

                rpmPoints.add(new int[]{rpm});
                powerPoints.add(new double[]{power});
                torquePoints.add(new double[]{torque});

                // Actualizar labels
                labelRPM.setText(String.valueOf(rpm));
                labelHP.setText(String.format("%.0f", power));
                labelNm.setText(String.format("%.0f", torque));

                // Redibujar
                drawChartBackground();
                drawCurves();
                drawGauge(rpm);
            }));
        }

        timeline.setOnFinished(e -> onDynoFinished());
        timeline.play();
    }

    private void onDynoFinished() {
        running  = false;
        finished = true;

        // Calcular resultado final
        dynoResult = dynoService.runDyno(car, car.getParts());

        labelHP.setText(String.format("%.0f", dynoResult.getMaxPower()));
        labelNm.setText(String.format("%.0f", dynoResult.getMaxTorque()));
        labelRPM.setText(String.valueOf(RPM_MAX));

        labelStatus.setText("✔  Prueba completada — Max " +
                String.format("%.0f", dynoResult.getMaxPower()) + " HP  /  " +
                String.format("%.0f", dynoResult.getMaxTorque()) + " Nm");
        labelStatus.setTextFill(Color.web("#00e676"));

        btnExport.setDisable(false);
        btnExport.setStyle(
                "-fx-background-color: #1a2a1a; -fx-text-fill: #00e676;" +
                        "-fx-border-color: #00e676; -fx-border-width: 1;" +
                        "-fx-font-family: Monospace; -fx-font-size: 12; -fx-padding: 8 20;" +
                        "-fx-cursor: hand;");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Dibujo del gráfico
    // ════════════════════════════════════════════════════════════════════════

    private static final double CHART_PAD_L = 55;
    private static final double CHART_PAD_R = 20;
    private static final double CHART_PAD_T = 20;
    private static final double CHART_PAD_B = 40;

    private void drawChartBackground() {
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        double w = chartCanvas.getWidth();
        double h = chartCanvas.getHeight();
        double cw = w - CHART_PAD_L - CHART_PAD_R;
        double ch = h - CHART_PAD_T - CHART_PAD_B;

        // Fondo
        gc.setFill(Color.web("#0d0d0f"));
        gc.fillRect(0, 0, w, h);

        // Área del gráfico
        gc.setFill(Color.web("#101018"));
        gc.fillRoundRect(CHART_PAD_L, CHART_PAD_T, cw, ch, 6, 6);

        // Grid horizontal (10 líneas)
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);
        int gridLines = 6;
        for (int i = 0; i <= gridLines; i++) {
            double y = CHART_PAD_T + ch * i / gridLines;
            gc.strokeLine(CHART_PAD_L, y, CHART_PAD_L + cw, y);

            // Etiqueta eje Y (HP aproximado)
            double val = maxPowerDisplay * (1.0 - (double) i / gridLines);
            gc.setFill(TEXT_GRAY);
            gc.setFont(Font.font("Monospace", 10));
            gc.fillText(String.format("%.0f", val), 4, y + 4);
        }

        // Grid vertical (RPM)
        int[] rpmMarks = {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000};
        for (int rpm : rpmMarks) {
            double x = rpmToX(rpm, cw);
            gc.setStroke(GRID_COLOR);
            gc.strokeLine(x, CHART_PAD_T, x, CHART_PAD_T + ch);
            gc.setFill(TEXT_GRAY);
            gc.setFont(Font.font("Monospace", 10));
            gc.fillText(rpm / 1000 + "k", x - 8, CHART_PAD_T + ch + 16);
        }

        // Eje X label
        gc.setFill(TEXT_GRAY);
        gc.setFont(Font.font("Monospace", 11));
        gc.fillText("RPM", CHART_PAD_L + cw / 2 - 12, h - 4);

        // Leyenda
        gc.setFill(ACCENT_RED);
        gc.fillRect(CHART_PAD_L + 10, CHART_PAD_T + 8, 18, 3);
        gc.setFill(TEXT_WHITE);
        gc.setFont(Font.font("Monospace", 10));
        gc.fillText("Potencia (HP)", CHART_PAD_L + 34, CHART_PAD_T + 13);

        gc.setFill(Color.web("#00c8ff"));
        gc.fillRect(CHART_PAD_L + 140, CHART_PAD_T + 8, 18, 3);
        gc.fillText("Torque (Nm)", CHART_PAD_L + 164, CHART_PAD_T + 13);
    }

    private void drawCurves() {
        if (powerPoints.isEmpty()) return;
        GraphicsContext gc = chartCanvas.getGraphicsContext2D();
        double cw = chartCanvas.getWidth() - CHART_PAD_L - CHART_PAD_R;
        double ch = chartCanvas.getHeight() - CHART_PAD_T - CHART_PAD_B;

        // ── Curva de potencia (rojo) ─────────────────────────────────────
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < powerPoints.size(); i++) {
            double x = rpmToX(rpmPoints.get(i)[0], cw);
            double y = valToY(powerPoints.get(i)[0], maxPowerDisplay, ch);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        // Punto actual (HP)
        int last = powerPoints.size() - 1;
        double lx = rpmToX(rpmPoints.get(last)[0], cw);
        double ly = valToY(powerPoints.get(last)[0], maxPowerDisplay, ch);
        gc.setFill(ACCENT_RED);
        gc.fillOval(lx - 4, ly - 4, 8, 8);

        // ── Curva de torque (azul) ───────────────────────────────────────
        gc.setStroke(Color.web("#00c8ff"));
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < torquePoints.size(); i++) {
            double x = rpmToX(rpmPoints.get(i)[0], cw);
            double y = valToY(torquePoints.get(i)[0], maxTorqueDisplay, ch);
            if (i == 0) gc.moveTo(x, y); else gc.lineTo(x, y);
        }
        gc.stroke();

        gc.setFill(Color.web("#00c8ff"));
        double tx = rpmToX(rpmPoints.get(last)[0], cw);
        double ty = valToY(torquePoints.get(last)[0], maxTorqueDisplay, ch);
        gc.fillOval(tx - 4, ty - 4, 8, 8);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Velocímetro / cuenta-RPM
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

        // Arco de fondo (gris)
        double startAngle = 220;
        double totalArc   = 280;
        gc.setStroke(Color.web("#1e1e2e"));
        gc.setLineWidth(10);
        gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, -totalArc,
                javafx.scene.shape.ArcType.OPEN);

        // Arco de progreso con gradiente rojo→amarillo
        double pct = Math.min(1.0, (double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN));
        if (pct > 0) {
            // Zona verde (0-60%)
            double arcGreen = totalArc * Math.min(pct, 0.6);
            gc.setStroke(Color.web("#00c853"));
            gc.setLineWidth(10);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, -arcGreen,
                    javafx.scene.shape.ArcType.OPEN);
            // Zona amarilla (60-85%)
            if (pct > 0.6) {
                double arcYel = totalArc * (Math.min(pct, 0.85) - 0.6);
                gc.setStroke(ACCENT_YEL);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle - totalArc * 0.6, -arcYel,
                        javafx.scene.shape.ArcType.OPEN);
            }
            // Zona roja (85-100%)
            if (pct > 0.85) {
                double arcRed = totalArc * (pct - 0.85);
                gc.setStroke(ACCENT_RED);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle - totalArc * 0.85, -arcRed,
                        javafx.scene.shape.ArcType.OPEN);
            }
        }

        // Marcas de RPM
        gc.setStroke(TEXT_GRAY);
        gc.setLineWidth(1.5);
        gc.setFill(TEXT_GRAY);
        gc.setFont(Font.font("Monospace", 9));
        for (int mark = RPM_MIN; mark <= RPM_MAX; mark += 1000) {
            double angle = Math.toRadians(startAngle - totalArc *
                    ((double)(mark - RPM_MIN) / (RPM_MAX - RPM_MIN)));
            double x1 = cx + (r - 8)  * Math.cos(angle);
            double y1 = cy - (r - 8)  * Math.sin(angle);
            double x2 = cx + r        * Math.cos(angle);
            double y2 = cy - r        * Math.sin(angle);
            gc.strokeLine(x1, y1, x2, y2);
            double xt = cx + (r - 20) * Math.cos(angle) - 6;
            double yt = cy - (r - 20) * Math.sin(angle) + 4;
            gc.fillText(mark / 1000 + "k", xt, yt);
        }

        // Aguja
        double needleAngle = Math.toRadians(startAngle -
                totalArc * Math.min(pct, 1.0));
        double nx = cx + (r - 18) * Math.cos(needleAngle);
        double ny = cy - (r - 18) * Math.sin(needleAngle);
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(2.5);
        gc.strokeLine(cx, cy, nx, ny);

        // Centro del gauge
        gc.setFill(Color.web("#222230"));
        gc.fillOval(cx - 7, cy - 7, 14, 14);
        gc.setStroke(ACCENT_RED);
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - 7, cy - 7, 14, 14);
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
    //  Cálculo de curvas
    // ════════════════════════════════════════════════════════════════════════

    private void buildFullCurves() {
        List<Part> parts = car.getParts();
        double power  = car.getBasePower();
        double torque = car.getBaseTorque();
        if (parts != null) {
            for (Part p : parts) {
                power  += p.getHpDelta();
                torque += p.getTorqueDelta();
            }
        }

        for (int rpm = RPM_MIN; rpm <= RPM_MAX; rpm += RPM_STEP) {
            double factor = Math.exp(-Math.pow((rpm - 5000) / 2000.0, 2));
            double p = power  * (0.6 + 0.4 * factor);
            double t = torque * (0.9 + 0.1 * factor) * (1.0 - (rpm - RPM_MIN) * 0.00003);
            fullPowerCurve.put(rpm, p);
            fullTorqueCurve.put(rpm, Math.max(t, 0));
        }

        maxPowerDisplay  = fullPowerCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(300) * 1.15;
        maxTorqueDisplay = fullTorqueCurve.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(400) * 1.15;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Coordenadas
    // ════════════════════════════════════════════════════════════════════════

    private double rpmToX(int rpm, double chartWidth) {
        return CHART_PAD_L + chartWidth *
                ((double)(rpm - RPM_MIN) / (RPM_MAX - RPM_MIN));
    }

    private double valToY(double val, double maxVal, double chartHeight) {
        return CHART_PAD_T + chartHeight * (1.0 - val / maxVal);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Exportar CSV
    // ════════════════════════════════════════════════════════════════════════

    private void exportCsv() {
        if (dynoResult == null) return;
        try {
            Path file = CsvExporter.exportDyno(car, dynoResult, Path.of("exports"));
            labelStatus.setText("✔  CSV exportado: " + file.getFileName());
            labelStatus.setTextFill(Color.web("#00e676"));
        } catch (Exception ex) {
            labelStatus.setText("✗  Error al exportar: " + ex.getMessage());
            labelStatus.setTextFill(ACCENT_RED);
        }
    }
}