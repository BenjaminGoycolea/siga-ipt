package cl.ipt.siga.controller;

import cl.ipt.siga.dto.StudyPlanRequest;
import cl.ipt.siga.dto.StudyPlanResponse;
import cl.ipt.siga.service.StudyPlanService;
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
@Tag(name = "Administración de planes de estudio", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminStudyPlanController {

    private final StudyPlanService studyPlanService;

    public AdminStudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @GetMapping("/careers/{careerId}/study-plans")
    @Operation(
            summary = "Listar planes de una carrera",
            description = "Devuelve los planes de estudio de la carrera indicada, ordenados por versión descendente."
    )
    public List<StudyPlanResponse> findByCareer(@PathVariable Long careerId) {
        return studyPlanService.findByCareer(careerId);
    }

    @PostMapping("/careers/{careerId}/study-plans")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Crear plan de estudio",
            description = """
                    Crea un plan asociado a la carrera indicada. Si currentPlan es true, el plan que estaba vigente
                    para esa carrera deja de estarlo. Esta operación no cambia el plan asociado a estudiantes ya
                    registrados.
                    """
    )
    public StudyPlanResponse create(
            @Parameter(description = "Identificador de la carrera a la que pertenecerá el plan.", example = "1")
            @PathVariable Long careerId,
            @Valid @RequestBody StudyPlanRequest request
    ) {
        return studyPlanService.create(careerId, request);
    }

    @GetMapping("/study-plans/{id}")
    @Operation(summary = "Consultar plan de estudio", description = "Devuelve los datos del plan indicado.")
    public StudyPlanResponse findById(@PathVariable Long id) {
        return studyPlanService.findById(id);
    }

    @PutMapping("/study-plans/{id}/current")
    @Operation(
            summary = "Establecer plan como vigente",
            description = """
                    Marca el plan indicado como vigente para su carrera y desactiva el plan vigente anterior.
                    No requiere cuerpo de solicitud.
                    """
    )
    public StudyPlanResponse setCurrent(@PathVariable Long id) {
        return studyPlanService.setCurrent(id);
    }
}
