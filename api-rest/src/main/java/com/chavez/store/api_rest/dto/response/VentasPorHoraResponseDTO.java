package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regla R-R-09: que franja horaria mueve mas volumen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VentasPorHoraResponseDTO {

    private List<VentaHoraDTO> porHora;
    private Integer horaPico;
    private Integer horaBaja;
    private BigDecimal ingresosTotales;
}