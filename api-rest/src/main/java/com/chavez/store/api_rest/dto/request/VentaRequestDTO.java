package com.chavez.store.api_rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chavez.store.api_rest.entity.enums.TipoVenta;

/**
 * Registro interno de venta. Sin pagos y sin voucher (regla R-V-14).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequestDTO {

    @NotNull(message = "El tipo de venta es obligatorio")
    private TipoVenta type;

    /** Obligatorio solo si type = CREDITO (validado en el servicio: R-V-13). */
    private Long clienteId;

    /** Permite registrar ventas de dias anteriores; nunca futuras (R-V-17). */
    @PastOrPresent(message = "La fecha de venta no puede ser futura")
    private LocalDateTime saleDate;

    @NotEmpty(message = "La venta debe tener al menos un item")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;
}