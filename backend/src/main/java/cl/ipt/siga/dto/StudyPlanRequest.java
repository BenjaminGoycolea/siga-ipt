package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "StudyPlanRequest", description = "Datos necesarios para crear un plan de estudio.")
public record StudyPlanRequest(
        @Schema(description = "Versión o identificador curricular dentro de la carrera.", example = "2021.3")
        @NotBlank @Size(max = 30) String version,
        @Schema(
                description = "Indica si el plan debe quedar vigente. Si es true, reemplaza al vigente de la carrera.",
                example = "false"
        )
        @NotNull Boolean currentPlan
) {
}
