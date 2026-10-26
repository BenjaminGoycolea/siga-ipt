package cl.ipt.siga.controller;

import cl.ipt.siga.dto.CourseEnrollmentResponse;
import cl.ipt.siga.dto.FinalGradeRequest;
import cl.ipt.siga.dto.SectionResponse;
import cl.ipt.siga.service.TeacherAcademicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/sections")
@PreAuthorize("hasRole('TEACHER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Gestión docente", description = "Consulta de estudiantes y registro de notas en secciones asignadas.")
public class TeacherAcademicController {

    private final TeacherAcademicService teacherAcademicService;

    public TeacherAcademicController(TeacherAcademicService teacherAcademicService) {
        this.teacherAcademicService = teacherAcademicService;
    }

    @GetMapping
    @Operation(
            summary = "Listar mis secciones",
            description = "Lista las secciones asignadas al docente autenticado para el período indicado."
    )
    public List<SectionResponse> findAssignedSections(
            @RequestParam Long periodId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherAcademicService.findAssignedSections(jwt.getSubject(), periodId);
    }

    @GetMapping("/{sectionId}/enrollments")
    @Operation(
            summary = "Listar estudiantes de una sección",
            description = "Devuelve las inscripciones activas de una sección asignada al docente autenticado."
    )
    public List<CourseEnrollmentResponse> findSectionEnrollments(
            @PathVariable Long sectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherAcademicService.findSectionEnrollments(jwt.getSubject(), sectionId);
    }

    @PutMapping("/{sectionId}/enrollments/{enrollmentId}/grade")
    @Operation(
            summary = "Registrar o corregir una nota final",
            description = "Guarda la calificación final entre 1,0 y 7,0 mientras el período esté abierto."
    )
    public CourseEnrollmentResponse updateFinalGrade(
            @PathVariable Long sectionId,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody FinalGradeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return teacherAcademicService.updateFinalGrade(
                jwt.getSubject(),
                sectionId,
                enrollmentId,
                request.finalGrade()
        );
    }
}
