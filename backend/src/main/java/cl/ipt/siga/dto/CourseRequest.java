package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        name = "CourseRequest",
        description = "Datos para crear o actualizar una asignatura. En una actualización, prerequisiteIds reemplaza "
                + "la lista completa de prerrequisitos."
)
public record CourseRequest(
        @Schema(description = "Código único dentro del plan de estudio.", example = "TAP201")
        @NotBlank @Size(max = 20) String code,
        @Schema(description = "Nombre de la asignatura.", example = "Programación Orientada a Objetos")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Semestre curricular, entre 1 y 4.", example = "2", minimum = "1", maximum = "4")
        @Min(1) @Max(4) int semester,
        @Schema(description = "Horas semanales de teoría; no puede ser negativo.", example = "2", minimum = "0")
        @Min(0) int theoryHours,
        @Schema(description = "Horas semanales de ejercicios; no puede ser negativo.", example = "2", minimum = "0")
        @Min(0) int exerciseHours,
        @Schema(description = "Horas semanales de laboratorio; no puede ser negativo.", example = "2", minimum = "0")
        @Min(0) int laboratoryHours,
        @Schema(description = "Créditos SCT, entre 1 y 7.", example = "6", minimum = "1", maximum = "7")
        @Min(1) @Max(7) int credits,
        @Schema(
                description = """
                        IDs de cero a tres asignaturas prerrequisito del mismo plan y de semestres anteriores.
                        Usa [] cuando no haya prerrequisitos.
                        """,
                example = "[10]"
        )
        @NotNull @Size(max = 3) List<@NotNull @Positive Long> prerequisiteIds
) {
}
