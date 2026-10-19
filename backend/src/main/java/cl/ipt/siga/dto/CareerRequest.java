package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CareerRequest", description = "Datos para crear o actualizar una carrera.")
public record CareerRequest(
        @Schema(description = "Código único de la carrera.", example = "10450")
        @NotBlank String code,
        @Schema(description = "Nombre de la carrera.", example = "Técnico Analista Programador")
        @NotBlank String name,
        @Schema(description = "Descripción de la carrera.", example = "Carrera técnica de análisis y programación.")
        @NotBlank String description,
        @Schema(description = "Año de inicio de la carrera.", example = "2020", minimum = "1")
        @Min(1) int startYear,
        @Schema(description = "Vacantes anuales disponibles.", example = "50", minimum = "1")
        @Min(1) int annualVacancies
) {
}
