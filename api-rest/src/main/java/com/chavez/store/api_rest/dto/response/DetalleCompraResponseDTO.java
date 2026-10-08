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
public class DetalleCompraResponseDTO {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private Long presentacionId;
    private String presentacionNombre;

    /** Factor de conversion de la presentacion (cajas x24 -> 24). */
    private Integer unitsBasePresentacion;

    /** Cantidad en la presentacion de compra (cajas), no en unidad base. */
    private Integer qtyBought;
    private Integer qtyReceived;

    /** Total en unidad base: qtyBought * unitsBasePresentacion. */
    private Integer unitsBase;

    /** Costo de una presentacion de compra (por caja). */
    private BigDecimal unitCost;

    /** unitCost / unitsBasePresentacion. Es el costo real por unidad base. */
    private BigDecimal costPorUnidadBase;

    private BigDecimal subtotal;
    private LocalDate expiryDate;
    private Long loteId;
    private String lotCode;
}