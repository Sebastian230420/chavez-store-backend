package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regla R-R-02: utilidad = ingresos - costoVentas - mermas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GananciasResponseDTO {

    private LocalDate desde;
    private LocalDate hasta;
    private Totales totales;
    private java.util.List<GananciaDiaDTO> porDia;
    private java.util.List<GananciaTipoVentaDTO> porTipoVenta;
    private BigDecimal recaudadoAbonos;
}