package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteDeudaDTO {

    private Long clienteId;
    private String document;
    private String nombre;
    private BigDecimal creditLimit;
    private BigDecimal saldoTotal;
    private BigDecimal disponible;
    private Antiguedad antiguedad;
    private LocalDate ultimaVenta;
    private LocalDate ultimoPago;
}