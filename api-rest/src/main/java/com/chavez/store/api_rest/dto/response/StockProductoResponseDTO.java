package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockProductoResponseDTO {

    private Long productoId;
    private String sku;
    private String nombre;
    private Integer stock;
    private Integer stockMin;
    private Integer stockMax;
    /** NORMAL | CRITICO | SIN_STOCK */
    private String estado;
    private BigDecimal valorInventario;
    private List<LoteStockResponseDTO> lotes;
}