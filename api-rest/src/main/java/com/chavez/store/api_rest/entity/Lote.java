package com.chavez.store.api_rest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lote de producto con fecha de vencimiento.
 * La salida de stock consume siempre el lote que vence primero (FEFO, regla R-I-10).
 */
@Entity
@Table(name = "lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_code", nullable = false, length = 60, unique = true)
    private String lotCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_detail_id")
    private DetalleCompra detalleCompra;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** NULL = producto sin vencimiento. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "qty_received", nullable = false)
    private Integer qtyReceived;

    @Column(name = "qty_remaining", nullable = false)
    private Integer qtyRemaining;

    /** Costo unitario congelado al ingreso del lote. */
    @Column(name = "cost_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal costUnit;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Un lote vencido no debe venderse si existe otro vigente (regla R-I-11). */
    public boolean isVencido() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public long diasParaVencer() {
        if (expiryDate == null) {
            return Long.MAX_VALUE;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}