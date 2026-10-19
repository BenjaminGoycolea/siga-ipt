package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Confirma si el estudiante está matriculado administrativamente en el período.")
public record MatriculationRequest(
        @Schema(description = "Estado de matrícula del período.", example = "true")
        @NotNull Boolean matriculated
) {
}
