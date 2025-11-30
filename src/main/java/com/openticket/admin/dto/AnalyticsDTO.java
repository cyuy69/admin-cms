package com.openticket.admin.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsDTO {

    private KPI kpi;
    private Overview overview;
    private LineCharts lineCharts;
    private TicketTypePie ticketPie;
    private Map<Long, CompareItem> compare;

    // ===================== KPI =====================
    @Data
    public static class KPI {
        private int todayViews;
        private int todaySales;
        private long weekRevenue;
        private double conversionRate;
    }

    // ===================== Overview =====================
    @Data
    public static class Overview {
        private long totalViews;
        private long totalSales;
        private long totalRevenue;
        private int totalEvents; // 你可以傳回所選活動數量
    }

    // ===================== Line Charts =====================
    @Data
    public static class LineCharts {
        private ChartData traffic; // 流量折線（合計）
        private ChartData sales; // 銷售折線（合計）
    }

    @Data
    public static class ChartData {
        private List<String> labels; // 日期
        private List<Integer> data; // 多活動 views/sales 加總後的值
    }

    // ===================== Pie Chart =====================
    @Data
    public static class TicketTypePie {
        private List<String> labels; // 票種名稱
        private List<Integer> data; // 合計數量（多活動加總）
    }

    @Data
    public static class CompareItem {
        private List<String> labels;
        private List<Integer> traffic; // 流量
        private List<Integer> sales; // 銷售
        private Map<String, Integer> pie; // 票種（可選）
        private KPI kpi; // （可選）
    }
}
