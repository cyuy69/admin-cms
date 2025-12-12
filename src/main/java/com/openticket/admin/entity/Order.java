package com.openticket.admin.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 對應的預訂單
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservations_id")
    private Reservation reservation;

    // 發票資訊
    @Column(name = "invoice_carrier_code")
    private String invoiceCarrierCode;

    @Column(name = "invoice_carrier_type")
    private String invoiceCarrierType;

    @Column(name = "invoice_donation_code")
    private String invoiceDonationCode;

    @Column(name = "invoice_tax_id")
    private String invoiceTaxId;

    @Column(name = "invoice_type")
    private String invoiceType;

    @Column(name = "invoice_value")
    private String invoiceValue;

    private String status;

    // 與 CheckoutOrder 的一對多
    @OneToMany(mappedBy = "order")
    private List<CheckoutOrder> checkoutOrders;

    @OneToMany(mappedBy = "order")
    private List<Payment> payments;

}
