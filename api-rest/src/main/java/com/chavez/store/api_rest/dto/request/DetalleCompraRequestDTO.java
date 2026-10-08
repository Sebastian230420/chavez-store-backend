package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class DetalleCompraRequestDTO {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La presentacion de compra es obligatoria")
    private Long presentacionId;

    /** Cantidad en la presentacion de COMPRA (cajas), no en unidad base. */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer qty;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(value = "0.0001", message = "El costo debe ser mayor a 0")
    private BigDecimal unitCost;

    /** Vencimiento del lote que se creara al recibir. */
    private LocalDate expiryDate;
}