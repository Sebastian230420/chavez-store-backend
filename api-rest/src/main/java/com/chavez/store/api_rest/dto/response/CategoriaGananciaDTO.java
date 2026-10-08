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
public class CategoriaGananciaDTO {

    private Long categoriaId;
    private String categoriaNombre;
    private Long ventasCount;
    private Long unidadesVendidas;
    private Double litrosVendidos;
    private BigDecimal ingresos;
    private BigDecimal costoVentas;
    private BigDecimal mermas;
    /** ingresos - costoVentas - mermas */
    private BigDecimal utilidad;
    /** (ingresos - costoVentas) / ingresos */
    private BigDecimal margenTeoricoPct;
    /** utilidad / ingresos */
    private BigDecimal margenRealPct;
    private BigDecimal participacionIngresosPct;
}