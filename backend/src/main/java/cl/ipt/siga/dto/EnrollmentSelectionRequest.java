package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        description = "Conjunto completo de secciones que el estudiante desea mantener inscritas en el período."
)
public record EnrollmentSelectionRequest(
        @Schema(description = "IDs de sección; entre 3 y 6, salvo que queden menos de tres asignaturas del plan.",
                example = "[1, 2, 3]")
        @NotEmpty @Size(max = 6) List<@NotNull Long> sectionIds
) {
}
