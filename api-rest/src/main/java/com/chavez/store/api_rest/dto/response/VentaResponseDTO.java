package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.EstadoVenta;
import com.chavez.store.api_rest.entity.enums.TipoVenta;

/**
 * Registro interno de venta. No incluye pagos ni voucher (regla R-V-14).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentaResponseDTO {

    private Long id;
    private String document;
    private TipoVenta type;
    private Long clienteId;
    private String clienteNombre;
    private String registradoPor;
    private LocalDateTime fecha;
    private BigDecimal subtotal;
    private BigDecimal total;
    private BigDecimal costTotal;
    private BigDecimal profit;
    private BigDecimal margenPct;
    private EstadoVenta status;
    private LocalDateTime annulledAt;
    private String annulReason;
    private List<DetalleVentaResponseDTO> detalles;
}