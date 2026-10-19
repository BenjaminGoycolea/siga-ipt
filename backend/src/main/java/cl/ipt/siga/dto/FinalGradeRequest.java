package cl.ipt.siga.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Calificación final determinada fuera del sistema.")
public record FinalGradeRequest(
        @Schema(description = "Nota final entre 1,0 y 7,0, con un decimal.", example = "5.5")
        @NotNull @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1)
        BigDecimal finalGrade
) {
}
