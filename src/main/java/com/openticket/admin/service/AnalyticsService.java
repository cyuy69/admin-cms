package com.openticket.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.openticket.admin.dto.AdminAnalyticsDTO;
import com.openticket.admin.dto.AnalyticsDTO;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventDailyStats;
import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.CheckoutOrderRepository;
import com.openticket.admin.repository.EventDailyStatsRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatsRepository;
import com.openticket.admin.repository.HomepageSessionLogRepository;
import com.openticket.admin.repository.PaymentRepository;

@Service
public class AnalyticsService {

    @Autowired
    private EventStatsRepository eventStatsRepository;

    @Autowired
    private EventDailyStatsRepository eventDailyStatsRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CheckoutOrderRepository checkoutOrderRepository;

    public AnalyticsDTO getAnalytics(
            List<Long> eventIds,
            String mode,
            LocalDate startDate,
            LocalDate endDate) {

        // (1) 修正時間區間
        LocalDate today = (endDate != null ? endDate : LocalDate.now());
        LocalDate start = (startDate != null ? startDate : today.minusDays(6));

        int days = (int) ChronoUnit.DAYS.between(start, today) + 1;

        // ================= DTO =================
        AnalyticsDTO dto = new AnalyticsDTO();
        dto.setKpi(new AnalyticsDTO.KPI());
        dto.setOverview(new AnalyticsDTO.Overview());
        dto.setLineCharts(new AnalyticsDTO.LineCharts());
        dto.setTicketPie(new AnalyticsDTO.TicketTypePie());
        dto.setCompare(new HashMap<>());

        // labels
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            labels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
        }

        // ================= 比較模式 =================
        if ("compare".equalsIgnoreCase(mode)) {

            for (Long id : eventIds) {

                AnalyticsDTO.CompareItem item = new AnalyticsDTO.CompareItem();

                // ==== 流量 ====
                int[] trafficArr = new int[days];
                List<EventDailyStats> dailyList = eventDailyStatsRepository.findByEventIdAndStatDateBetween(id, start,
                        today);

                for (EventDailyStats ds : dailyList) {
                    long diff = ChronoUnit.DAYS.between(start, ds.getStatDate());
                    if (diff >= 0 && diff < days) {
                        trafficArr[(int) diff] = ds.getDayViews();
                    }
                }

                item.setLabels(labels);
                item.setTraffic(Arrays.stream(trafficArr).boxed().toList());

                // ==== 銷售（日銷售量）====
                int[] salesArr = new int[days];
                LocalDateTime startTime = start.atStartOfDay();
                LocalDateTime endTime = today.plusDays(1).atStartOfDay();

                List<Object[]> salesRows = checkoutOrderRepository.findDailySalesByEvent(id, startTime, endTime);

                for (Object[] row : salesRows) {
                    LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
                    int qty = ((Number) row[1]).intValue();

                    long diff = ChronoUnit.DAYS.between(start, d);
                    if (diff >= 0 && diff < days) {
                        salesArr[(int) diff] = qty;
                    }
                }

                item.setSales(Arrays.stream(salesArr).boxed().toList());

                // ==== KPI ====
                int todayViews = eventDailyStatsRepository.sumTodayViews(id, today);
                int todaySales = salesArr[days - 1];

                AnalyticsDTO.KPI kpi = new AnalyticsDTO.KPI();
                kpi.setTodayViews(todayViews);
                kpi.setTodaySales(todaySales);
                kpi.setWeekRevenue(0);
                kpi.setConversionRate(todayViews == 0 ? 0 : (double) todaySales / todayViews);

                item.setKpi(kpi);

                // pie placeholder
                Map<String, Integer> pie = new LinkedHashMap<>();
                item.setPie(pie);
                dto.getCompare().put(id, item);
            }

            return dto;
        }

        // ================= merge 模式 =================

        int[] trafficSum = new int[days]; // 每天流量
        int[] salesSum = new int[days]; // 每天收入（金額）
        int[] ticketSum = new int[days]; // 每天售出票數（quantity）

        // 查付款（銷售/收入）
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        // 把每天的收入算進 salesSum
        List<Object[]> salesRaw = paymentRepository.findSalesBetween(eventIds, startTime, endTime);

        // Map<Date, 金額(用integer)>
        Map<LocalDate, Integer> salesMap = new HashMap<>();
        for (Object[] row : salesRaw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal amount = (BigDecimal) row[1]; // sum(unit_price * quantity)
            salesMap.put(date, amount == null ? 0 : amount.intValue());
        }

        // 補到days的長度
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            salesSum[i] = salesMap.getOrDefault(d, 0);
        }

        // trafficSum ( 每日流量 ) + ticketSum ( 每日售出票數 )
        for (Long id : eventIds) {

            // 流量:event_daily_stats
            List<EventDailyStats> dailyList = eventDailyStatsRepository.findByEventIdAndStatDateBetween(id, start,
                    today);

            for (EventDailyStats ds : dailyList) {
                long diff = ChronoUnit.DAYS.between(start, ds.getStatDate());
                if (diff >= 0 && diff < days) {
                    trafficSum[(int) diff] += ds.getDayViews();
                }
            }

            // 售出票數:
            List<Object[]> qtyRows = checkoutOrderRepository.findDailySalesByEvent(id, startTime, endTime);

            for (Object[] row : qtyRows) {
                LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
                int qty = ((Number) row[1]).intValue();
                long diff = ChronoUnit.DAYS.between(start, d);
                if (diff >= 0 && diff < days) {
                    ticketSum[(int) diff] += qty;
                }
            }
        }

        // 取得日期區間的總量
        long rangeViews = Arrays.stream(trafficSum).sum(); // 區間總瀏覽量
        long totalRevenue = Arrays.stream(salesSum).sum(); // 區間總收入
        long totalTickets = Arrays.stream(ticketSum).sum(); // 區間總售出票數

        // KPI 卡片
        dto.getKpi().setTodayViews((int) rangeViews); // 瀏覽量
        dto.getKpi().setTodaySales((int) totalTickets); // 售出票數
        dto.getKpi().setWeekRevenue(totalRevenue); // 收入
        dto.getKpi().setConversionRate(
                rangeViews == 0 ? 0.0 : (double) totalTickets / rangeViews);

        // overview : 維持原本總數據的邏輯
        long totalViews = 0;
        List<EventStats> statsList = eventStatsRepository.findByIdIn(eventIds);
        for (EventStats s : statsList) {
            totalViews += (s.getViews() != null ? s.getViews() : 0);
        }

        // 總收入
        long totalSales = totalRevenue;

        dto.getOverview().setTotalViews(totalViews);
        dto.getOverview().setTotalSales(totalSales);
        dto.getOverview().setTotalRevenue(totalRevenue);
        dto.getOverview().setTotalEvents(eventIds.size());

        // 折線圖
        AnalyticsDTO.ChartData trafficData = new AnalyticsDTO.ChartData();
        trafficData.setLabels(labels);
        trafficData.setData(Arrays.stream(trafficSum).boxed().toList());

        // 收入折線圖
        AnalyticsDTO.ChartData salesData = new AnalyticsDTO.ChartData();
        salesData.setLabels(labels);
        salesData.setData(Arrays.stream(salesSum).boxed().toList());

        dto.getLineCharts().setTraffic(trafficData);
        dto.getLineCharts().setSales(salesData);

        // 圓餅圖（真正的票種統計）
        List<Object[]> pieRows = checkoutOrderRepository.findTicketPieData(
                eventIds,
                startTime,
                endTime);

        Map<String, Integer> pieMap = new LinkedHashMap<>();

        for (Object[] row : pieRows) {
            String ticketName = String.valueOf(row[0]);
            int qty = ((Number) row[1]).intValue();
            pieMap.put(ticketName, qty);
        }

        // 放入 DTO
        dto.getTicketPie().setLabels(new ArrayList<>(pieMap.keySet()));
        dto.getTicketPie().setData(new ArrayList<>(pieMap.values()));

        return dto;

    }

    public AnalyticsDTO.Overview getTotalOverview(List<Long> eventIds) {

        AnalyticsDTO.Overview overview = new AnalyticsDTO.Overview();

        // 總瀏覽量
        long totalViews = eventStatsRepository.findByIdIn(eventIds)
                .stream()
                .mapToLong(s -> s.getViews() == null ? 0 : s.getViews())
                .sum();

        overview.setTotalViews(totalViews);

        // 總售出票數 + 總營收
        Object raw = checkoutOrderRepository.sumTotalTicketsAndRevenue(eventIds);

        Object[] row = raw != null ? (Object[]) raw : null;

        long totalSales = 0;
        long totalRevenue = 0;

        if (row != null) {
            if (row[0] != null)
                totalSales = ((Number) row[0]).longValue();

            if (row[1] != null)
                totalRevenue = ((Number) row[1]).longValue();
        }

        overview.setTotalSales(totalSales);
        overview.setTotalRevenue(totalRevenue);

        // 總活動數
        overview.setTotalEvents(eventIds.size());

        return overview;
    }

    // 查詢所有活動的(主要給管理者端使用)
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private HomepageSessionLogRepository homepageSessionLogRepository;

    public List<Long> getAllEventIds() {
        return eventRepository.findAll()
                .stream()
                .map(Event::getId)
                .toList();
    }

    public AdminAnalyticsDTO getAdminAnalytics(LocalDate startDate, LocalDate endDate) {

        // 1) 取得全部活動 ID
        List<Long> eventIds = getAllEventIds();

        // 時間區間處理
        LocalDate today = (endDate != null ? endDate : LocalDate.now());
        LocalDate start = (startDate != null ? startDate : today.minusDays(6));
        int days = (int) ChronoUnit.DAYS.between(start, today) + 1;

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        // 建立 DTO
        AdminAnalyticsDTO dto = new AdminAnalyticsDTO();

        // 查首頁總流量
        dto.homepageViews = homepageSessionLogRepository.count();

        // 查今日交易成功率
        long success = paymentRepository.findTodaySuccessCount();
        long total = paymentRepository.findTodayTotalCount();
        double successRate;

        if (total <= 0) {
            successRate = 0.0;
        } else {
            successRate = (double) success / total;
        }

        dto.successRate = successRate;

        // 每日流量折線圖
        int[] trafficArr = new int[days];
        List<EventDailyStats> statsList = eventDailyStatsRepository.findByEventIdInAndStatDateBetween(eventIds, start,
                today);

        for (EventDailyStats es : statsList) {
            long diff = ChronoUnit.DAYS.between(start, es.getStatDate());
            if (diff >= 0 && diff < days) {
                trafficArr[(int) diff] += es.getDayViews();
            }
        }

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            labels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
        }

        dto.traffic = new AnalyticsDTO.ChartData(labels,
                Arrays.stream(trafficArr).boxed().toList());

        // 每日成功交易筆數折線圖
        int[] txArr = new int[days];

        List<Object[]> txRows = paymentRepository.findDailySuccessTransactions(
                eventIds, startTime, endTime);

        for (Object[] row : txRows) {
            LocalDate d = ((java.sql.Date) row[0]).toLocalDate();
            long diff = ChronoUnit.DAYS.between(start, d);
            if (diff >= 0 && diff < days) {
                txArr[(int) diff] = ((Number) row[1]).intValue();
            }
        }

        dto.transactions = new AnalyticsDTO.ChartData(
                labels,
                Arrays.stream(txArr).boxed().toList());

        return dto;
    }

}
