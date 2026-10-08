package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.MetodoPago;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbonoResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private BigDecimal amount;
    private MetodoPago method;
    private Long appliedSaleId;
    private String appliedSaleDocument;
    private String notes;
    private String registradoPor;
    private LocalDateTime fecha;
    private LocalDateTime annulledAt;
    private BigDecimal saldoAnterior;
    private BigDecimal saldoActual;
}