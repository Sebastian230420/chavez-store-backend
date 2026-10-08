package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reporte de mermas (regla R-R-06): dinero que se perdio y por que. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MermasResponseDTO {

    private LocalDate desde;
    private LocalDate hasta;
    private Long unidades;
    private BigDecimal costoPerdido;
    private List<MermaMotivoDTO> porMotivo;
    private List<MermaProductoDTO> porProducto;
}