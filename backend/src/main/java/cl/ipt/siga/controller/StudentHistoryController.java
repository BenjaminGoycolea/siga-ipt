package cl.ipt.siga.controller;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.service.AcademicHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/academic-history")
@PreAuthorize("hasRole('STUDENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Historial académico", description = "Consulta del historial académico del estudiante autenticado.")
public class StudentHistoryController {

    private final AcademicHistoryService academicHistoryService;

    public StudentHistoryController(AcademicHistoryService academicHistoryService) {
        this.academicHistoryService = academicHistoryService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar mi historial académico",
            description = "Lista asignaturas, períodos, calificaciones y resultados de períodos ya cerrados."
    )
    public List<CourseEnrollmentResponse> findOwnHistory(@AuthenticationPrincipal Jwt jwt) {
        return academicHistoryService.findOwnHistory(jwt.getSubject());
    }
}
