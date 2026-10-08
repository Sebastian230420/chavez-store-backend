package com.chavez.store.api_rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class ClienteDetalleResponseDTO {

    private Long id;
    private String document;
    private String fullName;
    private BigDecimal creditLimit;
    private BigDecimal saldoActual;
    private BigDecimal disponible;
    private LocalDateTime ultimaVenta;
    private LocalDateTime ultimoAbono;
    private List<VentaResponseDTO> ventas;
    private List<AbonoResponseDTO> abonos;
}