package com.openticket.admin.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openticket.admin.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

        // 收入kpi卡片查詢邏輯
        @Query("""
                        SELECT DATE(p.createdAt) AS date,
                                SUM(co.unitPrice * co.quantity) AS revenue
                        FROM Payment p
                        JOIN p.order o
                        JOIN CheckoutOrder co ON co.order.id = o.id
                        JOIN co.eventTicketType ett
                        WHERE ett.event.id IN :eventIds
                        AND p.status IN ('SUCCESS','PAID')
                        AND p.createdAt >= :start
                        AND p.createdAt < :end
                        GROUP BY DATE(p.createdAt)
                        ORDER BY DATE(p.createdAt)
                        """)
        List<Object[]> findSalesBetween(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 2. 每日成功交易筆數(Admin使用)
        @Query("""
                        SELECT DATE(p.paidAt) AS day,
                                COUNT(p.id) AS count
                        FROM Payment p
                        JOIN p.order o
                        JOIN CheckoutOrder co ON co.order.id = o.id
                        JOIN co.eventTicketType ett
                        WHERE p.status = 'SUCCESS'
                                AND ett.event.id IN :eventIds
                                AND p.paidAt >= :start
                                AND p.paidAt < :end
                        GROUP BY DATE(p.paidAt)
                        ORDER BY DATE(p.paidAt)
                        """)
        List<Object[]> findDailySuccessTransactions(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // 今日成功交易筆數
        @Query("""
                        SELECT COUNT(p)
                        FROM Payment p
                        WHERE p.status = 'SUCCESS'
                        AND DATE(p.paidAt) = CURRENT_DATE
                        """)
        long findTodaySuccessCount();

        // 今日全部交易筆數(包含付款失敗、取消等等)
        @Query("""
                        SELECT COUNT(p)
                        FROM Payment p
                        WHERE DATE(p.createdAt) = CURRENT_DATE
                        """)
        long findTodayTotalCount();
}
