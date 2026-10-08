package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumenStock {

    private Integer productosStockCritico;
    private Integer productosSinStock;
    private Integer lotesPorVencer;
    private Integer lotesVencidos;
    private BigDecimal valorPorVencer;
    private BigDecimal valorVencido;
}