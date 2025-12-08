package com.openticket.admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openticket.admin.dto.OrderListDTO;
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
                        JOIN co.order o
                        JOIN Payment p ON p.order = o
                        JOIN co.eventTicketType ett
                        JOIN ett.ticketTemplate tt
                        WHERE ett.event.id IN :eventIds
                        AND p.status IN ('SUCCESS', 'PAID')
                        AND p.paidAt >= :startTime
                        AND p.paidAt < :endTime
                        GROUP BY tt.name
                        """)
        List<Object[]> findTicketPieData(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("""
                        SELECT new com.openticket.admin.dto.OrderListDTO(
                                o.id,
                                r.createdAt,
                                u.username,
                                e.title,
                                SUM(co.quantity),
                                SUM(co.quantity * co.unitPrice),
                                o.status
                        )
                        FROM CheckoutOrder co
                                JOIN co.order o
                                JOIN o.reservation r
                                JOIN r.user u
                                JOIN co.eventTicketType ett
                                JOIN ett.event e
                        WHERE e.id IN :eventIds
                                AND (
                                        :keyword IS NULL OR :keyword = ''
                                        OR u.username LIKE CONCAT('%', :keyword, '%')
                                        OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')
                                )
                        GROUP BY
                                o.id, r.createdAt, u.username, e.title, o.status
                        ORDER BY r.createdAt DESC
                        """)
        List<OrderListDTO> findOrdersByEvents(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("keyword") String keyword);

        // 查總售票+總營收
        @Query("""
                        SELECT
                        SUM(co.quantity),
                        SUM(co.quantity * co.unitPrice)
                        FROM CheckoutOrder co
                        JOIN co.order o
                        JOIN Payment p ON p.order = o
                        JOIN co.eventTicketType ett
                        WHERE ett.event.id IN :eventIds
                        AND p.status IN ('SUCCESS', 'PAID')
                        """)
        Object sumTotalTicketsAndRevenue(@Param("eventIds") List<Long> eventIds);

        // 查某個活動的總售票 + 總營收
        @Query("""
                            SELECT
                                COALESCE(SUM(co.quantity), 0),
                                COALESCE(SUM(co.quantity * co.unitPrice), 0)
                            FROM CheckoutOrder co
                            JOIN co.eventTicketType ett
                            JOIN co.order o
                            JOIN o.payments p
                            WHERE ett.event.id = :eventId
                              AND p.status IN ('SUCCESS','PAID')
                        """)
        List<Object[]> sumTicketsAndRevenueByEvent(@Param("eventId") Long eventId);

}
