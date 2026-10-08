package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de verificar el invariante de inventario (regla R-I-04).
 * products.stock debe igualar la suma de stock_movements.qty.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificarStockResponseDTO {

    private Boolean verificado;
    private Integer totalProductos;
    private Integer productosConDescuadre;
    private List<DescuadreDTO> descuadres;
}