package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.TipoMovimientoStock;

/** Fila del kardex de un producto, con el stock resultante despues del movimiento. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KardexResponseDTO {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long loteId;
    private String lotCode;
    private TipoMovimientoStock type;
    private Integer qty;
    private BigDecimal unitCost;
    private BigDecimal valorMovimiento;
    private String refTable;
    private Long refId;
    private String reason;
    private String usuario;
    private Integer stockResultante;
    private LocalDateTime createdAt;
}