package com.chavez.store.api_rest.entity;

import com.chavez.store.api_rest.entity.enums.TipoPresentacion;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Presentacion de un producto: factor de conversion a unidad base.
 * Cerveza Cristal 650ml -> Caja x24 (COMPRA, 24) + Unidad (VENTA, 1) + Pack x6 (VENTA, 6).
 */
@Entity
@Table(name = "product_presentations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Presentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 60)
    private String name;

    /** Cuantas unidades base representa esta presentacion. */
    @Column(name = "units_base", nullable = false)
    private Integer unitsBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPresentacion type;

    /** Precio de venta final. Solo aplica a presentaciones VENTA (regla R-C-06). */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "stock_min")
    private Integer stockMin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @PrePersist
    void prePersist() {
        if (active == null) {
            active = true;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}