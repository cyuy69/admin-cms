package com.openticket.admin.service;

import com.openticket.admin.dto.AnalyticsDTO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalyticsService {

    public AnalyticsDTO getAnalytics(List<Long> eventIds, int period, String mode) {

        // ========== 建 DTO ==========
        AnalyticsDTO dto = new AnalyticsDTO();
        dto.setKpi(new AnalyticsDTO.KPI());
        dto.setOverview(new AnalyticsDTO.Overview());
        dto.setLineCharts(new AnalyticsDTO.LineCharts());
        dto.setTicketPie(new AnalyticsDTO.TicketTypePie());
        dto.setCompare(new HashMap<>());

        // ========== 假資料來源 ==========
        Map<Long, Map<String, Object>> fake = new HashMap<>();

        fake.put(1L, Map.of(
                "labels", Arrays.asList("11/21", "11/22", "11/23", "11/24", "11/25", "11/26", "11/27"),
                "traffic", Arrays.asList(50, 60, 70, 90, 110, 130, 150),
                "sales", Arrays.asList(3, 4, 4, 6, 5, 8, 7),
                "todayViews", 30,
                "todaySales", 3,
                "weekRevenue", 8000,
                "totalViews", 1200,
                "totalSales", 150,
                "totalRevenue", 40000,
                "pie", Map.of("成人", 30, "兒童", 10, "VIP", 5, "自訂", 2)));

        fake.put(2L, Map.of(
                "labels", Arrays.asList("11/21", "11/22", "11/23", "11/24", "11/25", "11/26", "11/27"),
                "traffic", Arrays.asList(80, 100, 120, 150, 140, 170, 200),
                "sales", Arrays.asList(5, 7, 6, 10, 9, 12, 11),
                "todayViews", 50,
                "todaySales", 5,
                "weekRevenue", 10000,
                "totalViews", 1000,
                "totalSales", 200,
                "totalRevenue", 50000,
                "pie", Map.of("成人", 40, "兒童", 20, "VIP", 15, "自訂", 5)));

        // compare 模式：每個活動要分別輸出資料

        if ("compare".equals(mode)) {

            for (Long id : eventIds) {

                Map<String, Object> d = fake.get(id);
                if (d == null)
                    continue;

                AnalyticsDTO.CompareItem item = new AnalyticsDTO.CompareItem();

                item.setLabels((List<String>) d.get("labels"));
                item.setTraffic((List<Integer>) d.get("traffic"));
                item.setSales((List<Integer>) d.get("sales"));
                item.setPie((Map<String, Integer>) d.get("pie"));

                // 每活動自己的 KPI
                AnalyticsDTO.KPI kpi = new AnalyticsDTO.KPI();
                kpi.setTodayViews((int) d.get("todayViews"));
                kpi.setTodaySales((int) d.get("todaySales"));
                kpi.setWeekRevenue((int) d.get("weekRevenue"));
                kpi.setConversionRate(
                        ((int) d.get("todayViews") == 0) ? 0
                                : (double) (int) d.get("todaySales") / (int) d.get("todayViews"));
                item.setKpi(kpi);

                dto.getCompare().put(id, item);
            }

            return dto;
        }

        // merge 模式（你原本的合併統計）
        // ========== 合併結果初始化 ==========
        int[] trafficSum = new int[7];
        int[] salesSum = new int[7];

        Map<String, Integer> pieSum = new LinkedHashMap<>();
        pieSum.put("成人", 0);
        pieSum.put("兒童", 0);
        pieSum.put("VIP", 0);
        pieSum.put("自訂", 0);

        int todayViews = 0;
        int todaySales = 0;
        long weekRevenue = 0;
        long totalViews = 0;
        long totalSales = 0;
        long totalRevenue = 0;

        // ========== 開始合併多活動資料 ==========
        for (Long id : eventIds) {
            Map<String, Object> d = fake.get(id);
            if (d == null)
                continue;

            todayViews += (int) d.get("todayViews");
            todaySales += (int) d.get("todaySales");
            weekRevenue += (int) d.get("weekRevenue");
            totalViews += (int) d.get("totalViews");
            totalSales += (int) d.get("totalSales");
            totalRevenue += (int) d.get("totalRevenue");

            List<Integer> traffic = (List<Integer>) d.get("traffic");
            List<Integer> sales = (List<Integer>) d.get("sales");

            for (int i = 0; i < 7; i++) {
                trafficSum[i] += traffic.get(i);
                salesSum[i] += sales.get(i);
            }

            Map<String, Integer> pie = (Map<String, Integer>) d.get("pie");
            for (String k : pieSum.keySet()) {
                pieSum.put(k, pieSum.get(k) + pie.get(k));
            }
        }

        // ========== 填寫 DTO - KPI ==========
        dto.getKpi().setTodayViews(todayViews);
        dto.getKpi().setTodaySales(todaySales);
        dto.getKpi().setWeekRevenue(weekRevenue);
        dto.getKpi().setConversionRate(
                (todayViews == 0) ? 0 : (double) todaySales / todayViews);

        // ========== Overview ==========
        dto.getOverview().setTotalViews(totalViews);
        dto.getOverview().setTotalSales(totalSales);
        dto.getOverview().setTotalRevenue(totalRevenue);
        dto.getOverview().setTotalEvents(eventIds.size());

        // ========== 折線 labels ==========
        List<String> labels = (List<String>) fake.get(eventIds.get(0)).get("labels");

        AnalyticsDTO.ChartData trafficData = new AnalyticsDTO.ChartData();
        trafficData.setLabels(labels);
        trafficData.setData(Arrays.stream(trafficSum).boxed().toList());

        AnalyticsDTO.ChartData salesData = new AnalyticsDTO.ChartData();
        salesData.setLabels(labels);
        salesData.setData(Arrays.stream(salesSum).boxed().toList());

        dto.getLineCharts().setTraffic(trafficData);
        dto.getLineCharts().setSales(salesData);

        dto.getTicketPie().setLabels(new ArrayList<>(pieSum.keySet()));
        dto.getTicketPie().setData(new ArrayList<>(pieSum.values()));

        return dto;
    }
}
