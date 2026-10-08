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
public class Totales {

    private Long ventasCount;
    private Long unidadesVendidas;
    private BigDecimal ingresos;
    private BigDecimal costoVentas;
    private BigDecimal mermas;
    /** ingresos - costoVentas - mermas */
    private BigDecimal utilidadNeta;
    /** margen teorico sobre ingresos */
    private BigDecimal margenPct;
    /** margen real: descuenta tambien las mermas */
    private BigDecimal margenRealPct;
    /** utilidad por unidad base vendida */
    private BigDecimal utilidadPorUnidad;
}