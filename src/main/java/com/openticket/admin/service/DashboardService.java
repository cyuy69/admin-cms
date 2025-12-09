package com.openticket.admin.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.openticket.admin.dto.EventListItemDTO;
import com.openticket.admin.entity.Event;
import com.openticket.admin.entity.EventStats;
import com.openticket.admin.repository.CheckoutOrderRepository;
import com.openticket.admin.repository.EventRepository;
import com.openticket.admin.repository.EventStatsRepository;
import com.openticket.admin.service.event.EventService;

@Service
public class DashboardService {

        @Autowired
        private EventRepository eventRepository;

        @Autowired
        private EventStatsRepository eventStatsRepository;

        @Autowired
        private CheckoutOrderRepository checkoutOrderRepository;

        @Autowired
        private EventService eventService;

        /* 取得該最新 3 筆活動 */
        public List<EventListItemDTO> getLatestEvents(Long companyId) {

                // 取得活動
                List<Event> events = eventRepository.findByCompanyUser_Id(
                                companyId,
                                Sort.by(Sort.Direction.DESC, "createdAt"));

                return events.stream()
                                .limit(3)
                                .map(this::toDTO)
                                .toList();
        }

        // ========== 封裝：Event -> EventListItemDTO ==========
        private EventListItemDTO toDTO(Event e) {

                EventListItemDTO dto = new EventListItemDTO();

                dto.setId(e.getId());
                dto.setTitle(e.getTitle());
                dto.setEventStart(e.getEventStartFormatted());
                dto.setEventEnd(e.getEventEndFormatted());
                dto.setTicketStart(e.getTicketStartFormatted());
                dto.setCreatedAt(e.getCreatedAtIso());
                dto.setStatus(eventService.calculateDynamicStatus(e));
                dto.setImages(e.getImages());

                // ===== 從 event_stats 拿流量 & 分享 =====
                EventStats stats = eventStatsRepository.findById(e.getId()).orElse(null);
                dto.setViews(stats != null ? stats.getViews() : 0);
                dto.setShares(stats != null ? stats.getShares() : 0);

                // ===== 從 checkout_orders 取得售出票數 + 總營收 =====
                List<Object[]> rows = checkoutOrderRepository.sumTicketsAndRevenueByEvent(e.getId());

                if (rows != null && !rows.isEmpty()) {
                        Object[] row = rows.get(0);

                        dto.setTicketsSold(
                                        row[0] != null ? ((Number) row[0]).intValue() : 0);

                        dto.setRevenue(
                                        row[1] != null ? ((java.math.BigDecimal) row[1]).longValue() : 0L);

                } else {
                        dto.setTicketsSold(0);
                        dto.setRevenue(0L);
                }

                return dto;
        }

        public Map<String, Object> getOrganizerKpi(Long companyId) {

                List<Long> eventIds = eventRepository.findByCompanyUser_Id(companyId)
                                .stream()
                                .map(Event::getId)
                                .toList();

                if (eventIds.isEmpty()) {
                        return Map.of(
                                        "viewsTotal", 0,
                                        "ticketsTotal", 0,
                                        "revenueTotal", 0);
                }

                // A. 流量一次加總
                long viewsTotal = eventStatsRepository.sumViewsByEventIds(eventIds);

                // B. 售票＋營收一次加總
                Object result = checkoutOrderRepository.sumTotalTicketsAndRevenue(eventIds);
                Object[] row = result != null ? (Object[]) result : new Object[] { 0, 0 };

                long ticketsTotal = row[0] != null ? ((Number) row[0]).longValue() : 0;
                long revenueTotal = row[1] != null ? ((BigDecimal) row[1]).longValue() : 0;

                return Map.of(
                                "viewsTotal", viewsTotal,
                                "ticketsTotal", ticketsTotal,
                                "revenueTotal", revenueTotal);
        }

}
