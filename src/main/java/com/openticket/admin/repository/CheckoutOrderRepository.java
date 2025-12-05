package com.openticket.admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openticket.admin.dto.TicketSalesDTO;
import com.openticket.admin.entity.CheckoutOrder;

public interface CheckoutOrderRepository extends JpaRepository<CheckoutOrder, Long> {
        // 票卷銷售kpi卡片查詢邏輯
        @Query("""
                        SELECT DATE(p.paidAt) AS day,
                                SUM(co.quantity) AS qty
                        FROM CheckoutOrder co
                                JOIN co.order o
                                JOIN Payment p ON p.order = o
                                JOIN co.eventTicketType ett
                        WHERE p.status IN ('SUCCESS', 'PAID')
                                AND ett.event.id = :eventId
                                AND p.paidAt >= :start
                                AND p.paidAt <  :end
                        GROUP BY DATE(p.paidAt)
                        ORDER BY day
                        """)
        List<Object[]> findDailySalesByEvent(
                        @Param("eventId") Long eventId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 查詢票種名稱邏輯(圓餅圖要用)
        @Query("""
                        SELECT new com.openticket.admin.dto.TicketSalesDTO(
                            tt.name,
                            SUM(co.quantity)
                        )
                        FROM CheckoutOrder co
                        JOIN co.eventTicketType ett
                        JOIN ett.ticketTemplate tt
                        WHERE ett.event.id = :eventId
                        GROUP BY tt.name
                        """)
        List<TicketSalesDTO> findTicketSalesByEventId(@Param("eventId") Long eventId);

        @Query("""
                        SELECT tt.name AS ticketName,
                                SUM(co.quantity) AS totalQty
                        FROM CheckoutOrder co
                        JOIN co.eventTicketType ett
                        JOIN ett.ticketTemplate tt
                        JOIN co.order o
                        WHERE ett.event.id IN :eventIds
                            AND o.createdAt BETWEEN :startTime AND :endTime
                        GROUP BY tt.name
                        """)
        List<Object[]> findTicketPieData(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

}
