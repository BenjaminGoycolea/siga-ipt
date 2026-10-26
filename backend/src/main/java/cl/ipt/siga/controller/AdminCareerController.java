package cl.ipt.siga.controller;

import cl.ipt.siga.dto.CareerRequest;
import cl.ipt.siga.dto.CareerResponse;
import cl.ipt.siga.dto.CareerStatusRequest;
import cl.ipt.siga.service.CareerService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/admin/careers")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administración de carreras", description = "Operaciones disponibles para usuarios con rol ADMIN.")
public class AdminCareerController {

    private final CareerService careerService;

    public AdminCareerController(CareerService careerService) {
        this.careerService = careerService;
    }

    @GetMapping
    @Operation(summary = "Listar carreras", description = "Devuelve todas las carreras registradas.")
    public List<CareerResponse> findAll() {
        return careerService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar carrera", description = "Devuelve los datos de la carrera indicada.")
    public CareerResponse findById(@PathVariable Long id) {
        return careerService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Crear carrera",
            description = """
                    Crea una carrera activa de cuatro semestres. El código debe ser único; startYear y
                    annualVacancies deben ser mayores que cero.
                    """
    )
    public CareerResponse create(@Valid @RequestBody CareerRequest request) {
        return careerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar datos de carrera",
            description = """
                    Actualiza el código, nombre, descripción, año de inicio y vacantes anuales.
                    La duración permanece fija en cuatro semestres y el estado se modifica por separado.
                    """
    )
    public CareerResponse update(@PathVariable Long id, @Valid @RequestBody CareerRequest request) {
        return careerService.update(id, request);
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado de carrera",
            description = "Activa o inactiva la carrera. Las carreras inactivas no pueden recibir nuevos estudiantes."
    )
    public CareerResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CareerStatusRequest request
    ) {
        return careerService.updateStatus(id, request.status());
    }
}
