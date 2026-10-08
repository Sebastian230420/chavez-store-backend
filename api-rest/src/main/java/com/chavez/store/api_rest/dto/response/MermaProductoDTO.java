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
public class MermaProductoDTO {

    private Long productoId;
    private String nombre;
    private Long unidades;
    private BigDecimal costoPerdido;
    /** merma como porcentaje sobre las unidades vendidas del periodo. */
    private BigDecimal mermaSobreVentaPct;
}