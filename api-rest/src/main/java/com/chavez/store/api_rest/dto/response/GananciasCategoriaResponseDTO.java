package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ganancias por categoria (regla R-R-04).
 * La distincion clave: margenTeoricoPct vs margenRealPct. Un producto con 25% de
 * margen teorico pero 12% de merma tiene 13% de margen real.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GananciasCategoriaResponseDTO {

    private LocalDate desde;
    private LocalDate hasta;
    private BigDecimal totalIngresos;
    private BigDecimal totalUtilidad;
    private List<CategoriaGananciaDTO> categorias;
}