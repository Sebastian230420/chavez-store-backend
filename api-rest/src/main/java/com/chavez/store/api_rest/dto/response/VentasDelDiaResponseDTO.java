package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Total vendido por dia. No hay arqueo ni caja: solo agregacion de ventas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentasDelDiaResponseDTO {

    private String fecha;
    private ResumenDia resumen;
    private List<GananciaTipoVentaDTO> porTipoVenta;
    private List<AbonoDelDiaDTO> abonosDelDia;
    private List<TopProductoDTO> topProductos;
}