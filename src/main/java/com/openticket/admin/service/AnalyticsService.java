package com.openticket.admin.service;

import com.openticket.admin.dto.AnalyticsDTO;
import com.openticket.admin.entity.EventDailyStats;
import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.EventDailyStatsRepository;
import com.openticket.admin.repository.EventStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private EventStatsRepository eventStatsRepository;

    @Autowired
    private EventDailyStatsRepository eventDailyStatsRepository;

    public AnalyticsDTO getAnalytics(List<Long> eventIds, int period, String mode) {

        // ========== 建 DTO ==========
        AnalyticsDTO dto = new AnalyticsDTO();
        dto.setKpi(new AnalyticsDTO.KPI());
        dto.setOverview(new AnalyticsDTO.Overview());
        dto.setLineCharts(new AnalyticsDTO.LineCharts());
        dto.setTicketPie(new AnalyticsDTO.TicketTypePie());
        dto.setCompare(new HashMap<>());

        // ==========「還沒資料庫」的欄位，先一律給 0 ==========
        // 之後你有訂單 / 票種統計再來補

        // ====== 時間區間（先固定 7 天，period 你之後再用也行） ======
        int days = 7;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        // 做 labels：MM/dd
        List<String> labels = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            labels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
        }

        // ========== 比較模式：每個活動拆開看 ==========
        if ("compare".equalsIgnoreCase(mode)) {

            for (Long id : eventIds) {

                AnalyticsDTO.CompareItem item = new AnalyticsDTO.CompareItem();

                // 1. 折線圖：從 event_daily_stats 撈資料
                int[] trafficArr = new int[days];

                List<EventDailyStats> dailyList = eventDailyStatsRepository.findByEventIdAndStatDateBetween(id,
                        startDate, today);

                for (EventDailyStats ds : dailyList) {
                    long diff = ChronoUnit.DAYS.between(startDate, ds.getStatDate());
                    if (diff < 0 || diff >= days)
                        continue;
                    int index = (int) diff;
                    trafficArr[index] = ds.getDayViews();
                }

                item.setLabels(labels);
                item.setTraffic(Arrays.stream(trafficArr).boxed().toList());

                // 目前沒有訂單 → sales 先全 0
                item.setSales(Arrays.asList(0, 0, 0, 0, 0, 0, 0));

                // 圓餅圖也還沒有實際資料 → 全 0
                Map<String, Integer> pie = new LinkedHashMap<>();
                pie.put("成人", 0);
                pie.put("兒童", 0);
                pie.put("VIP", 0);
                pie.put("自訂", 0);
                item.setPie(pie);

                // 2. KPI：今天瀏覽量從 daily stats 算
                int todayViews = eventDailyStatsRepository.sumTodayViews(id, today);
                int todaySales = 0; // 沒訂單資料 → 先 0
                long weekRevenue = 0L; // 沒收入資料 → 先 0

                AnalyticsDTO.KPI kpi = new AnalyticsDTO.KPI();
                kpi.setTodayViews(todayViews);
                kpi.setTodaySales(todaySales);
                kpi.setWeekRevenue(weekRevenue);
                kpi.setConversionRate(
                        (todayViews == 0) ? 0.0 : (double) todaySales / todayViews);

                item.setKpi(kpi);

                dto.getCompare().put(id, item);
            }

            return dto;
        }

        // =====================================================================
        // 合併模式（merge）
        // =====================================================================

        // 合併結果初始化（保留你原本變數）
        int[] trafficSum = new int[days];
        int[] salesSum = new int[days]; // 現在沒資料，一律 0

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

        // 1. 總瀏覽量：從 event_stats 撈
        if (eventIds != null && !eventIds.isEmpty()) {
            List<EventStats> list = eventStatsRepository.findByIdIn(eventIds);
            for (EventStats s : list) {
                // 假設 EventStats 的 PK 是 eventId
                totalViews += (s.getViews() != null ? s.getViews() : 0);
                // shares 之後要用也可以在這邊順便加總
            }
        }

        // 2. 折線圖 + 今日瀏覽：從 event_daily_stats 撈
        for (Long id : eventIds) {

            List<EventDailyStats> dailyList = eventDailyStatsRepository.findByEventIdAndStatDateBetween(id, startDate,
                    today);

            for (EventDailyStats ds : dailyList) {
                long diff = ChronoUnit.DAYS.between(startDate, ds.getStatDate());
                if (diff < 0 || diff >= days)
                    continue;
                int idx = (int) diff;
                trafficSum[idx] += ds.getDayViews();
            }

            // KPI 用：合併所有勾選活動的今日瀏覽
            todayViews += eventDailyStatsRepository.sumTodayViews(id, today);
        }

        // （目前沒有訂單/收入 → todaySales / weekRevenue / totalSales / totalRevenue 都維持 0）

        // ========== 填寫 DTO - KPI ==========
        dto.getKpi().setTodayViews(todayViews);
        dto.getKpi().setTodaySales(todaySales); // 現在是 0
        dto.getKpi().setWeekRevenue(weekRevenue); // 現在是 0
        dto.getKpi().setConversionRate(
                (todayViews == 0) ? 0.0 : (double) todaySales / todayViews);

        // ========== Overview ==========
        dto.getOverview().setTotalViews(totalViews);
        dto.getOverview().setTotalSales(totalSales); // 現在是 0
        dto.getOverview().setTotalRevenue(totalRevenue); // 現在是 0
        dto.getOverview().setTotalEvents(eventIds.size());

        // ========== 折線圖 ==========
        AnalyticsDTO.ChartData trafficData = new AnalyticsDTO.ChartData();
        trafficData.setLabels(labels);
        trafficData.setData(Arrays.stream(trafficSum).boxed().toList());

        AnalyticsDTO.ChartData salesData = new AnalyticsDTO.ChartData();
        salesData.setLabels(labels);
        salesData.setData(Arrays.stream(salesSum).boxed().toList());

        dto.getLineCharts().setTraffic(trafficData);
        dto.getLineCharts().setSales(salesData);

        // ========== 圓餅圖（先全 0，之後有訂單再改） ==========
        dto.getTicketPie().setLabels(new ArrayList<>(pieSum.keySet()));
        dto.getTicketPie().setData(new ArrayList<>(pieSum.values()));

        return dto;
    }
}
