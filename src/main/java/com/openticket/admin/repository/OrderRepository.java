package com.openticket.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.openticket.admin.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT
                o.id AS orderId,
                r.created_at AS createdAt,
                u.username AS buyerName,
                e.title AS eventTitle,
                COALESCE(SUM(co.quantity), 0) AS ticketCount,
                r.totalAmount AS totalAmount,
                o.status AS status
            FROM orders o
            JOIN reservations r ON o.reservations_id = r.id
            JOIN user u ON r.user_id = u.id
            JOIN event e ON r.event_id = e.id
            LEFT JOIN checkout_orders co ON co.order_id = o.id
            WHERE r.event_id = :eventId
                AND (
                    :keyword IS NULL
                    OR u.username LIKE CONCAT('%', :keyword, '%')
                    OR CAST(o.id AS CHAR) LIKE CONCAT('%', :keyword, '%')
                    )
            GROUP BY
                o.id, r.created_at, u.username, e.title, r.totalAmount, o.status
            ORDER BY r.created_at DESC
            """, nativeQuery = true)
    List<Object[]> findOrdersByEventId(Long eventId, String keyword);
}
