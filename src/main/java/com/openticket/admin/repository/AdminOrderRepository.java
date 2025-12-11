package com.openticket.admin.repository;

import com.openticket.admin.dto.AdminOrderListDTO;
import com.openticket.admin.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT new com.openticket.admin.dto.AdminOrderListDTO(
                o.id,
                o.createdAt,
                u.username,
                u.account,
                e.title,
                SUM(co.quantity),
                o.status
            )
            FROM Order o
            JOIN o.reservation r
            JOIN r.user u
            JOIN o.checkoutOrders co
            JOIN co.eventTicketType ett
            JOIN ett.event e
            WHERE (:keyword IS NULL
                    OR u.username LIKE CONCAT('%', :keyword, '%')
                    OR u.account LIKE CONCAT('%', :keyword, '%')
                    OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%'))
                AND (:start IS NULL OR o.createdAt >= :start)
                AND (:end   IS NULL OR o.createdAt <= :end)
            GROUP BY o.id, o.createdAt, u.username, u.account, e.title, o.status
            ORDER BY o.createdAt DESC
            """)
    Page<AdminOrderListDTO> searchOrders(
            @Param("keyword") String keyword,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

}
