package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
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
public class StockReporteResponseDTO {

    private List<StockCriticoDTO> stockCritico;
    private List<LotePorVencerDTO> porVencer;
    private ResumenStock resumen;
}