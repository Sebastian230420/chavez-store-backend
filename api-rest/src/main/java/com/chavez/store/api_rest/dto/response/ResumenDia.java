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
public class ResumenDia {

    private Long ventasCount;
    private Long unidadesVendidas;
    private BigDecimal ingresosTotal;
    private BigDecimal costoTotal;
    private BigDecimal utilidad;
    private BigDecimal creditoOtorgado;
    private BigDecimal abonosRecibidos;
}