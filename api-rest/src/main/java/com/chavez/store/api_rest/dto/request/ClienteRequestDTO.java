package com.chavez.store.api_rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.chavez.store.api_rest.entity.enums.TipoCliente;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {

    /** DNI / RUC / CE. Unico y obligatorio (regla R-CL-01). */
    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 20, message = "El documento no puede superar 20 caracteres")
    private String document;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String fullName;

    private TipoCliente type;

    @Pattern(regexp = "^$|^[0-9]{6,15}$", message = "El telefono debe tener entre 6 y 15 digitos")
    private String phone;

    @Email(message = "El email no tiene un formato valido")
    private String email;

    @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
    private String address;

    /** Solo ADMIN puede modificarlo despues (regla R-CL-04). */
    private BigDecimal creditLimit;
}