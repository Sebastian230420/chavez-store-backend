package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Deuda de un cliente separada por antiguedad, parapriorizar la cobranza. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Antiguedad {

    /** 0 a 30 dias */
    private BigDecimal d0a30;

    /** 31 a 60 dias */
    private BigDecimal d31a60;

    /** 61 a 90 dias */
    private BigDecimal d61a90;

    /** mas de 90 dias */
    private BigDecimal mas90;

    /** Suma de todos los tramos. */
    public BigDecimal total() {
        BigDecimal suma = BigDecimal.ZERO;
        if (d0a30 != null) suma = suma.add(d0a30);
        if (d31a60 != null) suma = suma.add(d31a60);
        if (d61a90 != null) suma = suma.add(d61a90);
        if (mas90 != null) suma = suma.add(mas90);
        return suma;
    }
}