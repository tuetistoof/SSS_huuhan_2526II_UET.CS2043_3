package com.ssscloud.auction.client.util;

import javafx.geometry.Bounds;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;

import com.ssscloud.auction.common.payload.response.DTO.AuctionDTO;
import com.ssscloud.auction.common.payload.response.DTO.BidDTO;

public class PriceChartManager {

    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    private XYChart.Series<Number, Number> series;
    private int bidSequence = 0;

    private final Popup hoverPopup = new Popup();
    private final Label labelUsername = new Label();
    private final Label labelAmount  = new Label();
    private final Label labelSeq     = new Label();

    public PriceChartManager(LineChart<Number, Number> chart,
                             NumberAxis xAxis,
                             NumberAxis yAxis) {
        this.chart = chart;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        buildPopup();
    }

    public void rebuild(List<BidDTO> bidHistory, AuctionDTO auction) {
        series = new XYChart.Series<>();
        series.setName("Giá đấu");
        bidSequence = 0;

        for (int i = bidHistory.size() - 1; i >= 0; i--) {
            bidSequence++;
            BidDTO bid = bidHistory.get(i);
            XYChart.Data<Number, Number> dataPoint =
                    new XYChart.Data<>(bidSequence, bid.getBidAmount());
            installHover(dataPoint, bid.getBidderUsername(), bidSequence);
            series.getData().add(dataPoint);
        }

        configureYAxis(bidHistory, auction);
        configureXAxis();

        chart.getData().clear();
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
    }

    public void append(BidDTO bid) {
        if (series == null) return;
        bidSequence++;
        XYChart.Data<Number, Number> dataPoint =
                new XYChart.Data<>(bidSequence, bid.getBidAmount());
        installHover(dataPoint, bid.getBidderUsername(), bidSequence);
        series.getData().add(dataPoint);
        expandYAxisIfNeeded(bid.getBidAmount());
    }

    private void expandYAxisIfNeeded(long newPrice) {
        if (yAxis.isAutoRanging()) return;

        double currentUpper = yAxis.getUpperBound();
        double currentLower = yAxis.getLowerBound();
        double tickUnit     = yAxis.getTickUnit();

        boolean changed = false;

        if (newPrice > currentUpper) {
            double newUpper = Math.ceil((newPrice + tickUnit * 0.5) / tickUnit) * tickUnit;
            yAxis.setUpperBound(newUpper);
            changed = true;
        }

        if (newPrice < currentLower) {
            double newLower = Math.max(0, Math.floor((newPrice - tickUnit * 0.5) / tickUnit) * tickUnit);
            yAxis.setLowerBound(newLower);
            changed = true;
        }

        if (changed) {
            double range = yAxis.getUpperBound() - yAxis.getLowerBound();
            if (range > 0) {
                long rawTick   = (long) (range / 6);
                long magnitude = (long) Math.pow(10, (long) Math.log10(Math.max(rawTick, 1)));
                long newTick   = Math.max(magnitude, (long) tickUnit);
                yAxis.setTickUnit(newTick);
            }
        }
    }

    public void append(long bidAmount) {
        BidDTO stub = new BidDTO();
        stub.setBidAmount(bidAmount);
        stub.setBidderUsername("—");
        append(stub);
    }

    public boolean isReady() {
        return series != null;
    }

    private void buildPopup() {
        VBox card = new VBox(2, labelSeq, labelUsername, labelAmount);
        card.setStyle(
            "-fx-background-color: #1e1e2e;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #e74c7c;" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 8 12 8 12;"
        );

        labelSeq.setStyle("-fx-text-fill: #aaaacc; -fx-font-size: 11;");
        labelUsername.setStyle(
            "-fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: bold;"
        );
        labelAmount.setStyle(
            "-fx-text-fill: #e74c7c; -fx-font-size: 15; -fx-font-weight: bold;"
        );

        hoverPopup.getContent().add(card);
        hoverPopup.setAutoHide(true);
    }

    private void installHover(XYChart.Data<Number, Number> data,
                               String username, int seq) {
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode == null) return;
            newNode.setOnMouseEntered(e -> {
                newNode.setStyle("-fx-cursor: hand; -fx-scale-x: 1.6; -fx-scale-y: 1.6;");

                labelSeq.setText("Bid #" + seq);
                labelUsername.setText(username != null && !username.isBlank() ? username : "Ẩn danh");
                labelAmount.setText(String.format("%,d ₫", data.getYValue().longValue()));

                Bounds boundsInScene = newNode.localToScene(newNode.getBoundsInLocal());
                Bounds boundsInScreen = newNode.localToScreen(newNode.getBoundsInLocal());

                if (boundsInScreen != null) {
                    double popupX = boundsInScreen.getCenterX();
                    double popupY = boundsInScreen.getMinY() - 80;
                    hoverPopup.show(newNode, popupX - 60, popupY);
                }
            });

            newNode.setOnMouseExited(e -> {
                newNode.setStyle("-fx-scale-x: 1.0; -fx-scale-y: 1.0;");
                hoverPopup.hide();
            });
        });
    }

    private void configureYAxis(List<BidDTO> bidHistory, AuctionDTO auction) {
        if (auction == null || bidHistory.isEmpty()) {
            yAxis.setAutoRanging(true);
            return;
        }
        long minPrice = bidHistory.stream().mapToLong(BidDTO::getBidAmount).min().orElse(0);
        long maxPrice = bidHistory.stream().mapToLong(BidDTO::getBidAmount).max().orElse(0);
        long range    = maxPrice - minPrice;

        long rawTick   = range > 0 ? range / 6 : auction.getMinIncrement();
        long magnitude = (long) Math.pow(10, (long) Math.log10(Math.max(rawTick, 1)));
        long tickUnit  = Math.max(magnitude, auction.getMinIncrement());

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(Math.max(0, minPrice - tickUnit / 2.0));
        yAxis.setUpperBound(maxPrice + tickUnit / 2.0);
        yAxis.setTickUnit(tickUnit);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override public String toString(Number value) {
                if (value.longValue() >= 1_000_000) return (value.longValue() / 1_000_000) + "M";
                if (value.longValue() >= 1_000)     return (value.longValue() / 1_000)     + "K";
                return value.toString();
            }
        });
    }

    private void configureXAxis() {
        xAxis.setTickLabelFormatter(new javafx.util.StringConverter<>() {
            @Override public String toString(Number n)   { return "Bid " + n.intValue(); }
            @Override public Number fromString(String s) { return 0; }
        });
        xAxis.setMinorTickVisible(false);
        xAxis.setTickUnit(1);
    }
}