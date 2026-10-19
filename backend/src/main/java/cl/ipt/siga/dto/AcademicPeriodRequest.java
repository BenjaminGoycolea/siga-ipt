package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "AcademicPeriodRequest", description = "Año y semestre de un período académico.")
public record AcademicPeriodRequest(
        @Schema(description = "Año calendario.", example = "2027", minimum = "2000", maximum = "9999")
        @Min(2000) @Max(9999) int year,
        @Schema(description = "Semestre académico: 1 o 2.", example = "1", minimum = "1", maximum = "2")
        @Min(1) @Max(2) int semester
) {
}
