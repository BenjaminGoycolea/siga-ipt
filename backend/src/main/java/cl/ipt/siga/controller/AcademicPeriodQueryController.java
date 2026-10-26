package cl.ipt.siga.controller;

import cl.ipt.siga.dto.AcademicPeriodResponse;
import cl.ipt.siga.service.AcademicPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Períodos académicos", description = "Consulta de períodos para los perfiles académicos.")
public class AcademicPeriodQueryController {

    private final AcademicPeriodService academicPeriodService;

    public AcademicPeriodQueryController(AcademicPeriodService academicPeriodService) {
        this.academicPeriodService = academicPeriodService;
    }

    @GetMapping
    @Operation(
            summary = "Listar períodos académicos",
            description = "Lista períodos disponibles para consulta, inscripción, calificaciones y administración."
    )
    public List<AcademicPeriodResponse> findAll() {
        return academicPeriodService.findAll();
    }
}
