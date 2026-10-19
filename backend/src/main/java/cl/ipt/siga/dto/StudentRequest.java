package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StudentRequest", description = "Datos personales y carrera para registrar o actualizar un estudiante.")
public record StudentRequest(
        @Schema(description = "RUN del estudiante; debe ser único.", example = "12.345.678-9")
        @NotBlank String nationalId,
        @Schema(description = "Nombre completo del estudiante.", example = "María Pérez")
        @NotBlank String fullName,
        @Schema(description = "Correo electrónico válido y único.", example = "maria.perez@example.edu")
        @NotBlank @Email String email,
        @Schema(description = "ID de la carrera asociada. En creación se utiliza su plan vigente.", example = "1")
        @NotNull Long careerId
) {
}
