package com.chavez.store.api_rest.entity;

import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoVenta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro interno de venta. No guarda forma de pago ni voucher (regla R-V-14).
 * Documenta la salida de stock y la utilidad generada.
 */
@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correlativo por anio: V-2026-000001 */
    @Column(nullable = false, length = 20, unique = true)
    private String document;

    /** NULL en ventas de mostrador: no se identifica al consumidor final. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Cliente cliente;

    /** Quien registro el asiento. Obligatorio (regla R-V-16). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User usuario;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private TipoVenta type = TipoVenta.MOSTRADOR;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Sin impuestos: total == subtotal (regla R-V-04). */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /** Snapshot del costo de los items al momento de la venta (regla R-R-03). */
    @Column(name = "cost_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costTotal = BigDecimal.ZERO;

    /** total - costTotal, congelado al registrar (regla R-V-15). */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal profit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoVenta status = EstadoVenta.PAGADA;

    @Column(name = "annulled_at")
    private LocalDateTime annulledAt;

    @Column(name = "annul_reason", length = 255)
    private String annulReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annul_user_id")
    private User annulUser;

    @OneToMany(mappedBy = "venta", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<DetalleVenta> detalles = new java.util.ArrayList<>();

    @PrePersist
    void prePersist() {
        if (saleDate == null) {
            saleDate = LocalDateTime.now();
        }
        if (status == null) {
            status = EstadoVenta.PAGADA;
        }
        if (type == null) {
            type = TipoVenta.MOSTRADOR;
        }
    }

    public void agregarDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    public boolean isAnulada() {
        return status == EstadoVenta.ANULADA;
    }
}