package cl.ipt.siga.controller;

import cl.ipt.siga.dto.CourseRequest;
import cl.ipt.siga.dto.CourseResponse;
import cl.ipt.siga.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de asignaturas", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/study-plans/{studyPlanId}/courses")
    @Operation(
            summary = "Listar asignaturas de un plan",
            description = "Devuelve las asignaturas del plan indicado, ordenadas por semestre y código."
    )
    public List<CourseResponse> findByStudyPlan(@PathVariable Long studyPlanId) {
        return courseService.findByStudyPlan(studyPlanId);
    }

    @PostMapping("/study-plans/{studyPlanId}/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Agregar asignatura a un plan",
            description = """
                    Crea una asignatura dentro del plan indicado. prerequisiteIds debe contener los IDs de hasta
                    tres asignaturas del mismo plan que pertenezcan a semestres anteriores. La suma de horas
                    semanales TEL debe ser menor que 8.
                    """
    )
    public CourseResponse create(
            @Parameter(description = "Identificador del plan al que se agregará la asignatura.", example = "1")
            @PathVariable Long studyPlanId,
            @Valid @RequestBody CourseRequest request
    ) {
        return courseService.create(studyPlanId, request);
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Consultar asignatura", description = "Devuelve los datos de la asignatura indicada.")
    public CourseResponse findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @PutMapping("/courses/{id}")
    @Operation(
            summary = "Actualizar asignatura",
            description = """
                    Reemplaza los datos editables de la asignatura. En prerequisiteIds se deben enviar los IDs
                    de todos sus prerrequisitos deseados; usa una lista vacía para quitar todos. Se mantienen las
                    validaciones de plan, semestres, SCT y TEL.
                    """
    )
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }
}
