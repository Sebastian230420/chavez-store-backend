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
public class ResumenCobranza {

    private Integer clientesConDeuda;
    private BigDecimal saldoTotal;
    private BigDecimal deudaMas30Dias;
    private BigDecimal deudaMas90Dias;
}