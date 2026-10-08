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
public class ProductoGananciaDTO {

    private Long productoId;
    private String sku;
    private String nombre;
    private String categoria;
    private Long unidadesVendidas;
    private BigDecimal ingresos;
    private BigDecimal costo;
    private BigDecimal mermas;
    private BigDecimal utilidad;
    private BigDecimal margenTeoricoPct;
    private BigDecimal margenRealPct;
}