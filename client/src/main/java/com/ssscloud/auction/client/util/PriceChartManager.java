package com.ssscloud.auction.client.util;

import com.ssscloud.auction.common.dto.response.AuctionDTO;
import com.ssscloud.auction.common.dto.response.BidDTO;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * Tách chart logic ra khỏi BiddingRoomController.
 *
 * Dùng:
 *   chartManager = new PriceChartManager(priceLineChart, chartXAxis, chartYAxis);
 *   chartManager.rebuild(bidHistory, currentAuction); // gọi khi mở tab Chart
 *   chartManager.append(bid.getBidAmount());          // gọi khi có BID_UPDATE realtime
 */
public class PriceChartManager {

    private final LineChart<Number, Number> chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    private XYChart.Series<Number, Number> series;
    private int bidSequence = 0;

    public PriceChartManager(LineChart<Number, Number> chart,
                             NumberAxis xAxis,
                             NumberAxis yAxis) {
        this.chart = chart;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }


    public void rebuild(List<BidDTO> bidHistory, AuctionDTO auction) {
        series = new XYChart.Series<>();
        series.setName("Giá đấu");
        bidSequence = 0;

        for (int i = bidHistory.size() - 1; i >= 0; i--) {
            bidSequence++;
            series.getData().add(
                    new XYChart.Data<>(bidSequence, bidHistory.get(i).getBidAmount()));
        }

        configureYAxis(bidHistory, auction);
        configureXAxis();

        chart.getData().clear();
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
    }

    /** Thêm điểm mới realtime — chỉ gọi khi chart đang hiển thị. */
    public void append(long bidAmount) {
        if (series == null) return;
        bidSequence++;
        series.getData().add(new XYChart.Data<>(bidSequence, bidAmount));
    }

    public boolean isReady() {
        return series != null;
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