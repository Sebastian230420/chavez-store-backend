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
public class GananciaDiaDTO {

    private LocalDate fecha;
    private Long ventasCount;
    private Long unidadesVendidas;
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal mermas;
    private BigDecimal utilidad;
    private BigDecimal margenPct;
}