package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LotePorVencerDTO {

    private Long productoId;
    private String sku;
    private String nombre;
    private String lotCode;
    private Integer qtyRemaining;
    private LocalDate expiryDate;
    private Long diasRestantes;
    private BigDecimal valor;
}