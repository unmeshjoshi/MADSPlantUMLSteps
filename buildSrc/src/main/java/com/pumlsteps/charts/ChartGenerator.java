package com.pumlsteps.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.yaml.snakeyaml.Yaml;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ChartGenerator {
    
    public void generateChart(File configFile, File outputFile) throws IOException {
        generateChart(configFile, outputFile, "SVG");
    }
    
    public void generateChart(File configFile, File outputFile, String format) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> config;
        
        try (FileReader reader = new FileReader(configFile)) {
            config = yaml.load(reader);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        // Generate data series based on configuration
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seriesList = (List<Map<String, Object>>) config.get("series");
        if (seriesList != null) {
            for (Map<String, Object> seriesConfig : seriesList) {
                XYSeries series = new XYSeries((String) seriesConfig.get("name"));
                generateDataSeries(series, seriesConfig, config);
                dataset.addSeries(series);
            }
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            (String) config.get("title"),
            (String) config.get("xAxisLabel"),
            (String) config.get("yAxisLabel"),
            dataset,
            PlotOrientation.VERTICAL,
            (Boolean) config.getOrDefault("legend", true),
            true, false
        );
        
        // Apply styling and customizations
        customizeChart(chart, config);
        
        // Export in specified format
        Integer width = (Integer) config.getOrDefault("width", 800);
        Integer height = (Integer) config.getOrDefault("height", 600);
        
        if ("PNG".equalsIgnoreCase(format)) {
            exportChartAsPNG(chart, outputFile, width, height);
        } else {
            exportChartAsSVG(chart, outputFile, width, height);
        }
    }
    
    private void generateDataSeries(XYSeries series, Map<String, Object> seriesConfig, Map<String, Object> config) {
        Double mu = ((Number) config.getOrDefault("serviceRate", 1000.0)).doubleValue();
        
        Number xMinNum = (Number) seriesConfig.get("xMin");
        Number xMaxNum = (Number) seriesConfig.get("xMax");
        Number xStepNum = (Number) seriesConfig.get("xStep");
        
        double xMin = xMinNum != null ? xMinNum.doubleValue() : 0;
        double xMax = xMaxNum != null ? xMaxNum.doubleValue() : 1000;
        double xStep = xStepNum != null ? xStepNum.doubleValue() : 10;
        
        String formula = (String) seriesConfig.get("formula");
        
        for (double x = xMin; x <= xMax; x += xStep) {
            double y = calculateFormulaValue(formula, x, mu);
            if (!Double.isInfinite(y) && !Double.isNaN(y)) {
                series.add(x, y);
            }
        }
    }
    
    private double calculateFormulaValue(String formula, double lambda, double mu) {
        if (formula == null) return 0.0;
        
        switch (formula) {
            case "throughput_curve":
                if (lambda <= mu) {
                    return lambda;  // Linear phase
                } else {
                    // Beyond saturation: decline due to overhead
                    double overload = (lambda - mu) / mu;
                    double throughput = mu * (1 - 0.1 * overload);
                    return Math.max(0, throughput);
                }
                
            case "mm1_latency":
                double rho = lambda / mu;
                double serviceTime = 1.0 / mu;
                if (rho >= 1.0) return Double.POSITIVE_INFINITY;
                return (serviceTime / (1 - rho)) * 1000;  // Convert to milliseconds
                
            case "overload_latency":
                return 5000.0;  // High constant latency in overload
                
            default:
                return 0.0;
        }
    }
    
    private void customizeChart(JFreeChart chart, Map<String, Object> config) {
        XYPlot plot = chart.getXYPlot();
        
        // Apply markers
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> markers = (List<Map<String, Object>>) config.get("markers");
        if (markers != null) {
            for (Map<String, Object> marker : markers) {
                if ("vertical_line".equals(marker.get("type"))) {
                    String valueStr = (String) marker.get("value");
                    if (valueStr != null && valueStr.startsWith("${") && valueStr.endsWith("}")) {
                        String varName = valueStr.substring(2, valueStr.length() - 1);
                        Object varValue = config.get(varName);
                        if (varValue instanceof Number) {
                            double value = ((Number) varValue).doubleValue();
                            Color color = getAwtColor((String) marker.getOrDefault("color", "red"));
                            Number strokeWidth = (Number) marker.getOrDefault("strokeWidth", 2.0);
                            plot.addDomainMarker(new ValueMarker(value, color, 
                                new BasicStroke(strokeWidth.floatValue())));
                        }
                    }
                }
            }
        }
        
        // Apply series styling
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> seriesList = (List<Map<String, Object>>) config.get("series");
        if (seriesList != null) {
            for (int i = 0; i < seriesList.size(); i++) {
                Map<String, Object> seriesConfig = seriesList.get(i);
                Color color = getAwtColor((String) seriesConfig.getOrDefault("color", "blue"));
                Number strokeWidth = (Number) seriesConfig.getOrDefault("strokeWidth", 2.0);
                renderer.setSeriesPaint(i, color);
                renderer.setSeriesStroke(i, new BasicStroke(strokeWidth.floatValue()));
                renderer.setSeriesShapesVisible(i, false);
            }
        }
        
        // Apply chart styling
        plot.setBackgroundPaint(getAwtColor((String) config.getOrDefault("backgroundColor", "white")));
        if ((Boolean) config.getOrDefault("gridLines", false)) {
            plot.setDomainGridlinesVisible(true);
            plot.setRangeGridlinesVisible(true);
        }
        
        // Apply logarithmic axis if specified
        if ("logarithmic".equals(config.get("yAxisType"))) {
            String yAxisLabel = (String) config.get("yAxisLabel");
            plot.setRangeAxis(new LogarithmicAxis(yAxisLabel + " (log scale)"));
        }
    }
    
    private Color getAwtColor(String colorName) {
        if (colorName == null) return Color.BLUE;
        
        switch (colorName.toLowerCase()) {
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "orange": return Color.ORANGE;
            case "green": return Color.GREEN;
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            default: return Color.BLUE;
        }
    }
    
    private void exportChartAsSVG(JFreeChart chart, File outputFile, int width, int height) throws IOException {
        SVGGraphics2D svgGenerator = new SVGGraphics2D(width, height);
        chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, width, height));
        String svgElement = svgGenerator.getSVGElement();
        
        java.nio.file.Files.write(outputFile.toPath(), svgElement.getBytes());
    }
    
    private void exportChartAsPNG(JFreeChart chart, File outputFile, int width, int height) throws IOException {
        ChartUtils.saveChartAsPNG(outputFile, chart, width, height);
    }
} 