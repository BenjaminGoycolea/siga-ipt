# Guía de arquitectura y presentación

## 1. Qué problema resuelve SIGA IPT

SIGA IPT centraliza la gestión académica del instituto: estudiantes, carreras y planes, asignaturas y prerrequisitos, docentes, períodos y secciones, inscripciones, calificaciones e historial. Los tres perfiles del sistema son `ADMIN`, `TEACHER` y `STUDENT`.

La aplicación debe ser un monolito organizado por capas. Esto significa que frontend y backend son aplicaciones separadas dentro de un único repositorio, mientras que las funcionalidades del backend viven en una sola aplicación Spring Boot y se organizan por responsabilidad.

## 2. Estructura del repositorio

```text
backend/
  src/main/java/cl/ipt/siga/
    config/       Seguridad, autorización y configuración de la aplicación
    controller/   Endpoints REST; reciben solicitudes y producen respuestas
    dto/          Objetos de entrada y salida de la API
    entity/       Entidades persistidas y relaciones del dominio
    exception/    Errores de negocio y respuestas HTTP consistentes
    repository/   Acceso a datos con Spring Data JPA
    service/      Reglas de negocio y coordinación de operaciones
  src/test/       Pruebas unitarias y de integración
frontend/
  src/
    api/          Cliente HTTP y servicios para consumir la API
    components/   Componentes visuales reutilizables
    context/      Estado compartido, incluida la sesión
    pages/        Vistas de cada perfil y proceso
docs/             Material de apoyo para desarrollo y presentación
```

Las capas académicas incluyen entidades, repositorios, Controllers y Services para carreras, planes de estudio, asignaturas, estudiantes, docentes, períodos, secciones, inscripción, calificaciones e historial.

## 3. Cómo explicar las capas

- **Controller:** expone una operación HTTP. Debe validar el formato de la solicitud, invocar al servicio y traducir el resultado a un código HTTP. No debe contener reglas académicas.
- **Service:** implementa las reglas del enunciado y coordina repositorios. Por ejemplo, al inscribir asignaturas debe comprobar estado del estudiante, período, prerrequisitos, cupos, horarios y límites de carga.
- **Repository:** encapsula las consultas y escrituras de Spring Data JPA. No decide si una operación está permitida.
- **Entity:** representa datos persistentes y relaciones, como un estudiante asociado a un plan de estudios o una inscripción asociada a una sección.
- **DTO:** define lo que la API recibe y devuelve, evitando exponer directamente detalles internos de las entidades.
- **Frontend:** presenta vistas y formularios, llama la API mediante Axios y adapta la navegación al rol. La autorización real también se verifica en el backend; ocultar una vista no protege un endpoint.

## 4. Flujo de una solicitud

1. El usuario opera una vista React.
2. El cliente Axios envía una solicitud HTTP al backend. Si hay sesión, agrega el JWT emitido por Keycloak.
3. Spring Security valida la firma y vigencia del JWT usando las claves públicas de Keycloak y extrae los roles.
4. El Controller recibe la solicitud autorizada y delega la regla al Service.
5. El Service consulta o modifica información mediante Repository y JPA.
6. El Controller devuelve una respuesta HTTP que el frontend muestra al usuario.

`GET /api/health` es una excepción deliberada para verificar que la API responde; no expone información académica y no requiere autenticación.

## 5. Autenticación y autorización

Keycloak se encarga de autenticar usuario y contraseña y emitir el JWT. El backend se configura como OAuth2 Resource Server: valida los tokens y transforma los roles del claim `realm_access.roles` en autoridades de Spring (`ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`). El sistema no guarda contraseñas ni implementa un mecanismo de login propio.

Las rutas académicas se separarán por perfil (`/api/admin/**`, `/api/teacher/**`, `/api/student/**`) y tendrán reglas en Spring Security. El frontend utiliza Keycloak para iniciar/cerrar sesión y Axios para enviar el token. La configuración local de realm, cliente y roles vive en `infra/keycloak/realm-export/siga-ipt-realm.json`; Docker Compose inicia Keycloak junto a su PostgreSQL. Las instrucciones y el despliegue del proyecto usan Docker Engine con el plugin Docker Compose v2. Una instalación de Podman que provee un comando `docker compose` vía `podman-compose` no es equivalente al entorno documentado y puede fallar al resolver nombres cortos de imágenes.

### Ejecutar y probar Keycloak localmente

1. Copiar `infra/keycloak/.env.example` a `infra/keycloak/.env` y reemplazar los valores de ejemplo por contraseñas locales.
2. Desde la raíz, levantar los servicios con el comando de `README.md`. Keycloak importa el realm al primer arranque.
3. Abrir `http://localhost:8081/admin/`, iniciar sesión con las credenciales bootstrap y seleccionar el realm `siga-ipt`.
4. Crear un usuario de prueba, definir su contraseña y asignarle el rol de realm correspondiente.
5. Iniciar frontend y backend. El frontend usa `check-sso` silencioso; el login interactivo se activa desde el botón. El cliente público usa Authorization Code Flow con PKCE S256 y no contiene secreto en el navegador.

### Qué significa el redireccionamiento `prompt=none`

`check-sso` pregunta silenciosamente a Keycloak si ya existe una sesión; `prompt=none` solicita que no se muestre una pantalla de login. Sin un URI de comprobación silenciosa, el adaptador puede hacer una navegación de ida y vuelta al servidor de identidad. El frontend ahora apunta a `silent-check-sso.html` en un iframe y desactiva el fallback que navega a Keycloak. Si Keycloak está detenido, la portada queda disponible sin autenticar; el botón de login no funcionará hasta levantar Keycloak. La autenticación no se simula ni se omite para endpoints protegidos.

### Consideraciones para producción en la nube

- Usar una URL dedicada como `https://auth.example.edu`, DNS y TLS válido detrás de un proxy inverso. No exponer directamente el puerto interno 8080 de Keycloak a Internet.
- Cambiar `start-dev` por `start`, definir `KC_HOSTNAME=https://auth.example.edu`, `KC_PROXY_HEADERS=xforwarded`, `KC_HTTP_ENABLED=true` solo si TLS termina en el proxy, y restringir el acceso de red al servicio.
- Configurar Keycloak con PostgreSQL persistente, credenciales desde el gestor de secretos del proveedor, copias de seguridad y actualizaciones controladas. No usar la base de desarrollo H2 ni contraseñas del `.env.example`.
- Añadir a la URI pública las cabeceras proxy correctas y configurar `KEYCLOAK_ISSUER_URI` en backend para que coincida exactamente con el `iss` de los JWT. El JWK Set URI debe corresponder al mismo realm.
- En el cliente web, cambiar `redirectUris` y `webOrigins` a los orígenes HTTPS exactos del frontend; no dejar comodines globales en producción. `VITE_KEYCLOAK_URL` se incorpora al compilar el frontend.
- Mantener la importación JSON para aprovisionar realm/roles/cliente de forma repetible, pero revisar el cliente de producción y los redirect URI específicos del dominio antes del despliegue.

## 6. Épicas y reglas centrales

1. **Estudiantes:** RUN y correo únicos, estado inicial `REGULAR`, vínculo a carrera y plan vigente; vinculación segura de identidad Keycloak y confirmación de matrícula por período.
2. **Carreras y planes:** código único, cuatro semestres, planes históricos preservados y un solo plan vigente por carrera.
3. **Asignaturas y prerrequisitos:** códigos únicos dentro del plan, semestre 1–4, SCT de 1–7, TEL no negativo con suma menor que 8 y hasta tres prerrequisitos del mismo plan en semestres anteriores.
4. **Docentes:** RUN y correo únicos; solo docentes activos pueden responsabilizarse de nuevas secciones. La administración permite registrar, consultar, actualizar y cambiar estado; no se puede desactivar a un docente con sección en un período abierto.
5. **Períodos y secciones:** una sección por asignatura y período, cupo máximo 60, horarios válidos y sin conflictos de docente. Las secciones con inscripciones no pueden cambiar asignatura/horario ni quedar con capacidad menor a sus ocupantes.
6. **Inscripciones:** estudiante autenticado, regular y matriculado; período abierto; plan y prerrequisitos válidos; 3–6 asignaturas salvo que queden menos; prioridad a cursos inferiores disponibles; cupos y horarios compatibles.
7. **Calificaciones y cierre:** nota final entre 1,0 y 7,0; aprobación desde 4,0; cierre solo con todas las notas registradas; reprobar dos veces una asignatura lleva a `DISMISSED` (ELIMINADO) y completar el plan a `GRADUATED` (EGRESADO).

Estas reglas pertenecen principalmente a Services y deben probarse allí. Las operaciones de inscripción y cierre modifican varias entidades relacionadas, por lo que también necesitan pruebas de integración transaccionales.

### Matriz de pruebas de integración

Las pruebas con `@SpringBootTest` y `MockMvc` comprueban tres procesos de extremo a extremo. Se contabilizan escenarios de respuesta distintos dentro de los flujos encadenados, ya que cada solicitud necesita los datos creados por los pasos anteriores.

| Proceso | Escenarios exitosos | Escenarios de rechazo |
|---|---|---|
| Matrícula e inscripción (`StudentAcademicControllerTest`) | Marcar matrícula; guardar una selección válida; anular una inscripción propia. | Inscribir con prerrequisitos pendientes; seleccionar horarios incompatibles; seleccionar menos del mínimo de asignaturas. |
| Calificaciones y cierre (`StudentAcademicControllerTest`) | Consultar estudiantes inscritos; registrar una nota aprobatoria; registrar una nota reprobatoria y cerrar el período con historial disponible. | Rechazar nota fuera de rango; impedir que un docente consulte una sección ajena; impedir el cierre si faltan notas. |
| Preparación de oferta (`AdminCurriculumControllerTest`, `AdminOfferingControllerTest`) | Crear un plan vigente; agregar una asignatura; crear un período y su sección. | Rechazar una asignatura inválida; rechazar un período con semestre inválido; impedir desactivar al docente asignado a una sección abierta. |

Cada proceso contempla al menos tres respuestas exitosas y tres de rechazo, para un mínimo de 18 escenarios verificables entre sus solicitudes. Los tests también comprueban las restricciones de roles y el estado irreversible de un período cerrado.

## 7. Modelo académico implementado

- Una **Career** tiene código único, estado, duración, año de inicio y vacantes anuales.
- Una **StudyPlan** pertenece a una carrera y su versión es única dentro de ella. `StudyPlanService` serializa los cambios por carrera y desactiva el plan anterior al activar uno nuevo.
- Un **Course** pertenece a un plan, tiene código único dentro de ese plan, semestre, horas TEL y SCT. `CourseService` valida que los prerrequisitos sean del mismo plan y de semestres anteriores, y evita modificar el semestre de una asignatura si invalida prerrequisitos existentes. No hay eliminación física de planes o asignaturas.
- Un **Student** se asocia a carrera y plan, inicia en estado `REGULAR`, tiene RUN/correo únicos y se vincula a una cuenta mediante el claim `sub` de Keycloak.
- Un **Teacher** tiene RUN/correo únicos, título, grado, estado inicial `ACTIVE` y vínculo opcional al `sub` de Keycloak.
- Un **AcademicPeriod** se identifica por año y semestre (1 o 2), con código visible como `2027-1`; parte `OPEN`.
- Una **Section** ofrece una asignatura en un período con un docente, capacidad de 1–60 y bloques de horario. La combinación asignatura/período es única.
- Una **StudentMatriculation** registra la matrícula administrativa externa por estudiante/período. Un **CourseEnrollment** asocia estudiante, asignatura, sección y período; conserva fecha de registro, estado de anulación y nota final. El resultado `PASSED` o `FAILED` se deriva automáticamente de la nota.

El modelo deja varias reglas inter-entidad al Service deliberadamente: anotaciones y restricciones SQL ayudan con formato y unicidad, pero no sustituyen reglas de negocio como comprobar que un prerrequisito pertenece al mismo plan y a un semestre anterior.

El inicializador de perfil `dev` inserta las carreras 10450, 10451 y 10452 si aún no existen. También carga los planes completos TAP 2021.3, TAD 2022.3 y TCS 2021.3, cada uno con 24 asignaturas. El prerrequisito corregido de `TCS301` es `TCS204`.

## 8. Primera funcionalidad: administración de estudiantes

El Controller `AdminStudentController` publica las operaciones bajo `/api/admin/students` y restringe todo el controlador al rol `ADMIN`. El Controller recibe/valida DTOs y delega; `StudentService` implementa las reglas. `StudentRepository`, `CareerRepository` y `StudyPlanRepository` buscan/persisten la información.

Al crear un estudiante se normalizan RUN y correo, se comprueba que no estén repetidos, se verifica que la carrera exista y esté activa, y se vincula el plan vigente. El estado inicial es `REGULAR`. El servicio permite cambiar datos personales, pero no la carrera, porque la transferencia entre carreras aún no está soportada. El administrador puede asociar el `sub` de Keycloak y registrar la matrícula por período. `GRADUATED` y `DISMISSED` se asignan automáticamente durante el cierre académico.

Los errores se traducen a respuestas HTTP: `404` para recursos inexistentes, `409` para conflictos de reglas y `400` para validaciones de entrada. Las pruebas de servicio cubren las reglas y las pruebas MockMvc verifican autenticación, rol y respuestas HTTP.

## 9. Administración de carreras

`AdminCareerController` expone `/api/admin/careers`, protegido para `ADMIN`. `CareerService` lista, consulta, crea y actualiza datos de carreras, y permite cambiar su estado `ACTIVE`/`INACTIVE`. Al crear, el estado inicial es `ACTIVE` y la duración es siempre cuatro semestres. El código se normaliza a mayúsculas y se valida su unicidad. No existe eliminación física.

## 10. Gestión de planes y asignaturas

`AdminStudyPlanController` permite listar planes por carrera, crear un plan y asignar cuál está vigente. La operación de activación bloquea la carrera durante la transacción para que dos cambios simultáneos no dejen más de un plan vigente; el plan anterior de la misma carrera se desactiva automáticamente. El historial de cada estudiante conserva su vínculo al plan original.

`AdminCourseController` permite listar, consultar, crear y actualizar asignaturas. `CourseService` valida los datos SCT/TEL, la unicidad del código por plan y las relaciones de prerrequisitos. Estos deben pertenecer al mismo plan y a semestres anteriores; además, no permite mover una asignatura de semestre si eso invalida prerrequisitos existentes. Se omite la eliminación física para no borrar estructura académica referenciada; una asignatura con calificaciones en períodos cerrados tampoco puede modificarse, para mantener fiel el historial académico.

## 11. Administración de docentes

`AdminTeacherController` expone `/api/admin/teachers` solo para `ADMIN`. `TeacherService` permite listar (con filtro opcional por estado), consultar, registrar, actualizar datos, cambiar estado y vincular una cuenta Keycloak por su `sub`. RUN, correo y cuenta se verifican como únicos; un docente nuevo queda `ACTIVE`. No existe eliminación física. La regla que impide desactivar docentes con secciones en períodos abiertos consulta las asignaciones existentes.

## 12. Períodos y oferta académica

`AdminAcademicPeriodController` gestiona períodos únicos por año/semestre, inicialmente `OPEN`. Cerrar no es un cambio administrativo simple: exige nota final en todas las inscripciones activas, conserva los resultados en el historial y actualiza estado; un período cerrado no se puede reabrir. `AdminSectionController` permite listar por período, crear y actualizar secciones. `SectionService` exige período abierto, docente activo, capacidad de 1–60, una sola sección por asignatura/período y bloques horarios válidos y no repetidos. Los bloques deben sumar las horas TEL semanales de la asignatura, contando dos horas pedagógicas por bloque; también se rechazan colisiones de día/módulo entre secciones del mismo docente y período. Una sección con estudiantes inscritos no puede cambiar asignatura/horario ni reducir su capacidad por debajo de sus ocupantes.

## 13. Inscripción de asignaturas

`StudentAcademicController` expone oferta, consulta y selección completa de inscripciones bajo `/api/student/periods/{periodId}`. El controlador exige el rol `STUDENT`; el servicio resuelve al estudiante mediante el claim `sub` del JWT y nunca acepta un ID de estudiante enviado por el cliente. Un administrador vincula la cuenta desde `/api/admin/students/{id}/keycloak-subject` y registra matrícula externa del período en `/api/admin/students/{studentId}/periods/{periodId}/matriculation`.

`StudentEnrollmentService` solo permite modificar la selección en períodos `OPEN` a estudiantes `REGULAR` y matriculados. Verifica plan, asignaturas ya aprobadas, prerrequisitos, prioridad de asignaturas inferiores ofrecidas, carga de 3–6 cursos (con al menos una asignatura si quedan menos de tres en el plan), cupos y conflictos de horario. La operación reemplaza la selección completa dentro de una transacción: las asignaturas omitidas quedan `CANCELLED`; también existe una anulación individual mientras el período esté abierto y la inscripción no tenga nota. Un bloqueo pesimista sobre estudiante y secciones serializa cambios concurrentes que podrían exceder cupos o carga.

### Calificaciones e historial

`TeacherAcademicController` exige `TEACHER`; el docente se resuelve por el `sub` del JWT y solo opera secciones que tiene asignadas. Una nota final entre 1,0 y 7,0 puede registrarse o corregirse mientras el período esté abierto; `CourseEnrollmentResponse` deriva `PASSED` para notas desde 4,0 y `FAILED` para las inferiores. El historial estudiantil solo muestra inscripciones activas calificadas cuyo período ya está cerrado.

Al cerrar, `AcademicPeriodService` bloquea el período y comprueba primero las notas faltantes; si existen, informa las secciones afectadas sin cambiar datos. Si la validación pasa, cierra el período en la misma transacción, marca `DISMISSED` al estudiante que reprueba por segunda vez la misma asignatura y `GRADUATED` cuando ya aprobó todos los cursos de su plan. Una vez cerrado, el período y sus notas quedan inmutables mediante las operaciones normales. El historial se consulta con `GET /api/student/academic-history` o `GET /api/admin/students/{id}/academic-history`.

## 14. Decisiones de la primera etapa

- **JDK 17 y Spring Boot 3:** base compatible con las dependencias del curso y la referencia.
- **H2 en desarrollo:** simplifica el arranque local. La configuración de producción usará PostgreSQL; el esquema de producción debe evolucionar con migraciones antes del despliegue.
- **React, Vite, Tailwind, Axios y React Router:** separan presentación, estilos, llamadas HTTP y navegación.
- **Keycloak y JWT:** delegan las credenciales y permiten autorizar por roles sin almacenar contraseñas en SIGA IPT.
- **OpenAPI:** documenta los endpoints para inspeccionarlos y probarlos desde Swagger UI.

## 15. Portales web por rol

La aplicación tiene navegación por los roles del token Keycloak y páginas para `STUDENT`, `TEACHER` y `ADMIN`. `RoleRoute` mejora la experiencia de navegación, pero la seguridad no depende del frontend: Spring Security y `@PreAuthorize` vuelven a verificar cada solicitud. La consulta compartida de períodos está en `GET /api/academic-periods`.

- **Estudiante:** consulta períodos y oferta, guarda o anula su selección y revisa el historial de períodos cerrados. No elige su propia identidad: el backend relaciona el JWT con el estudiante mediante `sub`.
- **Docente:** consulta únicamente secciones asignadas a su cuenta vinculada, revisa estudiantes inscritos y registra o corrige la nota final. Los campos de nota se deshabilitan al cerrar el período; el backend también impide la escritura.
- **Administración:** registra estudiantes y docentes, vincula sus cuentas, marca matrícula institucional, crea períodos y prepara secciones de la oferta. La pestaña de catálogos permite crear/actualizar carreras, crear planes y elegir cuál está vigente, y crear/actualizar asignaturas con sus prerrequisitos. Al escoger una asignatura para una sección, el portal presenta las horas TEL separadas en teoría, ejercicios y laboratorio, su total semanal y los bloques requeridos (dos horas pedagógicas cada uno). El cierre de período informa conflictos de notas pendientes y es irreversible.

### Orden sugerido para demostrar el flujo

1. En Keycloak, asignar los roles y crear cuentas de prueba; en administración, registrar estudiantes y docentes y vincular cada ficha con el claim `sub` del token correcto.
2. Crear un período abierto, marcar la matrícula del estudiante y preparar secciones para asignaturas del plan. Revisar TEL y la cantidad de bloques que indica el portal; después asignar docente y bloques sin conflictos.
3. Iniciar sesión como estudiante y seleccionar asignaturas. Mostrar que el backend aplica prerrequisitos, carga mínima/máxima, cupos y horarios.
4. Iniciar sesión como docente y registrar notas finales en su sección. Intentar cerrar el período con notas pendientes para mostrar el conflicto; completar notas y cerrarlo.
5. Volver al portal estudiante y mostrar el historial. La interfaz refleja los resultados, pero las reglas y permisos se hacen cumplir en la API.

## 16. Guion breve para la presentación

> “SIGA IPT es una aplicación monolítica por capas que centraliza procesos académicos. El frontend React consume una API REST Spring Boot. Los Controllers manejan HTTP, los Services concentran reglas de negocio, los Repositories usan JPA para persistir y Keycloak gestiona la identidad. Spring Security valida el JWT y autoriza según ADMIN, TEACHER o STUDENT. Por ejemplo, el docente solo registra notas en sus secciones y el cierre exige que todas las inscripciones activas estén calificadas; después, el sistema actualiza el historial y la situación académica.”

Al presentar, conviene abrir Swagger UI para mostrar la API, explicar una regla concreta desde su Service y su prueba, y demostrar cómo el rol limita una operación tanto en backend como en frontend.

## 17. Estado y orden de implementación

- **Etapa 1 — base del proyecto:** estructura, configuración local, endpoint de salud, OpenAPI y adaptador inicial de Keycloak. Completa.
- **Etapa 2 — modelo académico:** entidades y repositorios de carreras, planes, asignaturas, estudiantes y docentes; seed de carreras y los planes TAP 2021.3, TAD 2022.3 y TCS 2021.3. Completa.
- **Etapa 3 — administración académica:** endpoints ADMIN implementados para estudiantes, carreras, planes de estudio, asignaturas, docentes, períodos y secciones.
- **Etapa 4 — inscripción académica:** identidad Keycloak, matrícula por período, oferta, inscripción/anulación y validación de reglas. Completa en backend.
- **Etapa 5 — calificaciones y cierre:** registro de notas por el docente asignado, cierre con validación, actualización de estados e historial. Completa en backend.
- **Etapa 6 — interfaz por rol y autorización completa:** portales de inscripción/historial, calificaciones y administración de carreras, planes, asignaturas, períodos, secciones y matrículas conectados a la API; navegación basada en roles y permisos verificados en backend.
- **Etapa 7 — calidad y entrega:** cobertura de líneas de servicios ≥90% y matriz de al menos 18 escenarios de integración comprobadas con `mvn verify`. El workflow de GitHub Actions ya ejecuta la verificación y publica las imágenes del backend y frontend en Docker Hub solo después de pasar. Pendiente el despliegue manual en un servidor cloud y completar sus dominios, variables y secretos.

No se deben dar por completas las etapas futuras hasta que su implementación y pruebas estén presentes en el repositorio.
