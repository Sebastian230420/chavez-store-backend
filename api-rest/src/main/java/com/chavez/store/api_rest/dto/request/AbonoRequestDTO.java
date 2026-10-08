package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.chavez.store.api_rest.entity.enums.MetodoPago;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbonoRequestDTO {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotNull(message = "El metodo de pago es obligatorio")
    private MetodoPago method;

    /** Abono a una venta concreta. NULL = a cuenta general (regla R-CL-11). */
    private Long appliedSaleId;

    @Size(max = 255, message = "La nota no puede superar 255 caracteres")
    private String notes;
}