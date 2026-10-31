# SIGA IPT

Sistema Integrado de Gestión Académica del Instituto Profesional de Tecnología.

El proyecto es un monorepo con un frontend React/Vite y una API Java/Spring Boot. El código de la aplicación se escribe en inglés; esta documentación está en español para facilitar el aprendizaje y la presentación del proyecto.

## Estado actual

En el perfil local se cargan tres carreras y los planes TAP 2021.3, TAD 2022.3 y TCS 2021.3, con 24 asignaturas cada uno. La API y los portales web permiten a `ADMIN` administrar estudiantes, docentes, períodos y secciones; estudiantes autenticados pueden inscribirse y consultar su historial; docentes vinculados a Keycloak registran notas en sus secciones. El cierre valida notas pendientes y actualiza historial y estado académico. La cobertura de líneas de los servicios supera el 90%, la suite de integración cubre los escenarios requeridos y el workflow de CI/CD está configurado. Falta ejecutar la publicación con credenciales propias y desplegar manualmente en un servidor cloud.

## Ejecutar localmente

Requisitos: Docker Engine con el plugin Docker Compose v2, JDK 17 o superior, Maven, Node.js y npm. Este proyecto documenta y valida el despliegue con Docker Compose; Podman no es un requisito.

1. Iniciar Keycloak y su PostgreSQL desde la raíz del proyecto:

   ```bash
   cp infra/keycloak/.env.example infra/keycloak/.env
   # Edita infra/keycloak/.env y define contraseñas locales propias.
   docker compose --env-file infra/keycloak/.env -f infra/keycloak/docker-compose.yml up -d
   ```

   Antes, verifica que `docker compose version` reporte Docker Compose v2 y que `docker info` muestre un servidor Docker. Si `docker compose` anuncia que ejecuta `podman-compose`, el comando está conectado a Podman y no a Docker Engine. Keycloak queda en `http://localhost:8081`; su consola administrativa está en `/admin/`. El primer arranque puede tardar un minuto.

2. Iniciar el backend desde `backend/`:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   El perfil local usa una base H2 en memoria para que la API pueda iniciarse sin instalar PostgreSQL. La documentación OpenAPI queda en `http://localhost:8080/swagger-ui.html`.

3. Iniciar el frontend desde `frontend/`:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Abrir `http://localhost:5173`. Vite reenvía `/api` al backend local. La portada consulta `GET /api/health`; después de iniciar sesión, la navegación muestra los portales permitidos por los roles del token.

La página de inicio no exige sesión. La configuración `check-sso` comprueba la sesión de forma silenciosa, por lo que Keycloak no debería sacar al usuario de la portada; el botón **Iniciar sesión** sí necesita que Keycloak esté activo.

El realm se importa con `ADMIN`, `TEACHER`, `STUDENT` y el cliente público `siga-ipt-web`. Para probar el login, entra a la consola como el administrador inicial definido en `infra/keycloak/.env`, selecciona el realm `siga-ipt`, crea un usuario de prueba, define una contraseña no temporal y asígnale uno de los roles de realm. No uses las credenciales de ejemplo en producción.

## Portales web por rol

La interfaz requiere que Keycloak esté activo y que el usuario inicie sesión. Los enlaces **Estudiante**, **Docente** y **Administración** aparecen solo si el JWT incluye el rol correspondiente; la API vuelve a comprobar cada permiso, por lo que ocultar un enlace no reemplaza la autorización del backend.

- **Estudiante:** elige un período abierto, revisa la oferta habilitada para su plan y guarda o anula inscripciones. La selección completa se valida en el backend (matrícula del período, prerrequisitos, carga mínima/máxima, cupos y horarios). La pestaña de historial muestra resultados de períodos cerrados.
- **Docente:** elige un período y una de sus secciones, consulta sus estudiantes y registra o corrige notas finales entre 1,0 y 7,0. Los períodos cerrados quedan en modo de solo lectura.
- **Administración:** registra estudiantes y docentes, vincula cada ficha con el claim `sub` de su usuario Keycloak, marca matrícula institucional por período, crea/cierra períodos y crea secciones a partir de carreras, planes, asignaturas y docentes existentes. En la pestaña **Carreras y asignaturas** puede crear y actualizar carreras, crear planes y cambiar cuál está vigente, y agregar o actualizar asignaturas con sus prerrequisitos. Al crear una sección, el formulario muestra el desglose TEL (teoría-ejercicios-laboratorio), el total semanal y cuántos bloques requiere; cada bloque representa dos horas pedagógicas. Los bloques se ingresan separados por coma, por ejemplo `L2, W3`, y el backend verifica cantidad, formato y conflictos.

Para vincular una persona, asígnale primero en Keycloak el rol `STUDENT` o `TEACHER` correspondiente. Copia el claim `sub` del token de esa cuenta (no el nombre de usuario ni el correo) y pégalo en la ficha del portal de administración. La persona estudiante también debe tener la matrícula del período marcada antes de inscribirse. Para cerrar un período, primero deben quedar calificadas todas las inscripciones activas; el cierre no se puede revertir.

La lista general de períodos utilizada por los portales está disponible para usuarios autenticados en `GET /api/academic-periods`.

## Primer endpoint académico: estudiantes

Las rutas de administración requieren un JWT de Keycloak con el rol de realm `ADMIN`. `GET /api/admin/students` lista estudiantes; admite `?careerId={id}` para filtrar. También están disponibles `GET /api/admin/students/{id}`, `POST /api/admin/students`, `PUT /api/admin/students/{id}` y `PUT /api/admin/students/{id}/status`.

El alta recibe `nationalId`, `fullName`, `email` y `careerId`. El sistema valida los campos, evita RUN/correos duplicados, y asocia automáticamente el plan vigente de la carrera. El cambio de carrera no está disponible. Los estados terminales `GRADUATED` y `DISMISSED` se reservan para el proceso de cierre de período.

## Planes de estudio y asignaturas

Las operaciones también requieren el rol `ADMIN` y están disponibles en Swagger:

- `GET /api/admin/careers/{careerId}/study-plans` lista los planes de una carrera.
- `POST /api/admin/careers/{careerId}/study-plans` crea un plan con `version` y `currentPlan`. Si se crea como vigente, el plan vigente anterior de esa carrera deja de estarlo.
- `GET /api/admin/study-plans/{id}` consulta un plan y `PUT /api/admin/study-plans/{id}/current` lo establece como vigente, conservando el plan asociado a cada estudiante.
- `GET` y `POST /api/admin/study-plans/{studyPlanId}/courses` listan y agregan asignaturas. `GET` y `PUT /api/admin/courses/{id}` consultan y actualizan una asignatura.

Una asignatura se envía con `code`, `name`, `semester`, `theoryHours`, `exerciseHours`, `laboratoryHours`, `credits` y `prerequisiteIds` (IDs de asignaturas del mismo plan). Se validan códigos únicos por plan, los cuatro semestres, SCT de 1 a 7, TEL no negativo con suma menor que 8, y hasta tres prerrequisitos de semestres anteriores. No se ofrece eliminación física de planes ni asignaturas. Una asignatura con calificaciones de períodos cerrados no se puede modificar, para conservar los datos mostrados en historiales académicos.

## Administración de docentes

Los endpoints `/api/admin/teachers` requieren el rol `ADMIN`:

- `GET /api/admin/teachers` lista docentes; acepta `?status=ACTIVE` o `?status=INACTIVE`.
- `GET /api/admin/teachers/{id}` consulta un docente.
- `POST /api/admin/teachers` registra docente con `nationalId`, `fullName`, `email`, `professionalTitle` y `academicDegree`. RUN y correo deben ser únicos, y el estado inicial queda `ACTIVE`.
- `PUT /api/admin/teachers/{id}` actualiza esos datos personales y profesionales; el estado no cambia.
- `PUT /api/admin/teachers/{id}/status` cambia el estado con un JSON como `{"status":"INACTIVE"}`.

No hay eliminación física de docentes. La regla de no desactivar a quien tenga secciones asignadas en períodos abiertos se aplica al administrar períodos y secciones. Para habilitar el registro de notas, `PUT /api/admin/teachers/{id}/keycloak-subject` asocia el docente a su claim `sub` de Keycloak; el mismo usuario debe tener el rol `TEACHER`.

## Períodos y secciones

Las rutas `/api/admin/periods/**` y `/api/admin/sections/**` requieren el rol `ADMIN`:

- `GET /api/admin/periods` lista los períodos; `GET /api/admin/periods/{id}` consulta uno.
- `POST /api/admin/periods` crea un período con `year` y `semester` (1 o 2). El código se muestra como `2027-1` y el estado inicial es `OPEN`; el año/semestre no se puede repetir.
- `PUT /api/admin/periods/{id}/status` acepta `{"status":"CLOSED"}` para ejecutar el cierre académico, o `{"status":"OPEN"}` mientras el período aún no se haya cerrado.
- `GET /api/admin/periods/{periodId}/sections` lista la oferta de un período.
- `POST /api/admin/periods/{periodId}/sections` crea una sección; `GET /api/admin/sections/{id}` consulta una y `PUT /api/admin/sections/{id}` actualiza su configuración.

La sección recibe `courseId`, `teacherId`, `capacity` y `scheduleBlocks`, por ejemplo `["L2","W3"]`. Solo se permite modificar secciones en períodos abiertos; la asignatura debe ser única dentro del período, el docente debe estar activo, la capacidad va de 1 a 60 y los bloques (L/M/W/J/V + módulo 1–6) deben ser únicos, sin conflictos del docente, y corresponder a las horas TEL de la asignatura (dos horas por bloque). Un docente con secciones en períodos abiertos no puede pasar a INACTIVE. No hay eliminación física de secciones. Una sección con estudiantes inscritos no puede reducir su cupo por debajo de los inscritos ni cambiar de asignatura u horario.

## Inscripción académica

Las rutas de administración requieren `ADMIN`:

- `PUT /api/admin/students/{id}/keycloak-subject` vincula un estudiante con el claim `sub` de su usuario Keycloak. El identificador se obtiene del JWT del estudiante; una misma cuenta no puede vincularse a dos estudiantes.
- `PUT /api/admin/students/{studentId}/periods/{periodId}/matriculation` registra el estado de matrícula institucional externa a SIGA IPT, con `{"matriculated":true}` o `false`.

El estudiante debe tener asignado el rol `STUDENT` y su cuenta Keycloak debe estar vinculada. Sus rutas usan el `sub` del token, no un `studentId` recibido en la URL:

- `GET /api/student/periods/{periodId}/offerings` lista secciones del plan para las que los prerrequisitos están aprobados.
- `GET /api/student/periods/{periodId}/enrollments` consulta sus inscripciones y anulaciones.
- `PUT /api/student/periods/{periodId}/enrollments` recibe la selección completa, por ejemplo `{"sectionIds":[1,2,3]}`. Reemplazar la selección anula las asignaturas que se quitaron; toda modificación exige período `OPEN`.
- `DELETE /api/student/periods/{periodId}/enrollments/{enrollmentId}` anula una inscripción propia del estudiante autenticado.

El servicio valida estado `REGULAR`, matrícula, plan de estudios, aprobación previa, prerrequisitos, prioridad de asignaturas de semestres inferiores disponibles, mínimo de 3 y máximo de 6 asignaturas (con excepción si quedan menos de tres en el plan), cupos y horarios. Cada cambio de selección es transaccional; las secciones se bloquean al verificar cupos para evitar sobreinscripciones concurrentes.

## Calificaciones, cierre e historial académico

El registro de notas requiere una cuenta Keycloak vinculada a un docente y con rol `TEACHER`. El docente solo puede consultar y calificar sus propias secciones:

- `GET /api/teacher/sections/{sectionId}/enrollments` lista estudiantes con inscripción activa.
- `PUT /api/teacher/sections/{sectionId}/enrollments/{enrollmentId}/grade` guarda o corrige la nota final con un decimal entre 1,0 y 7,0; el cuerpo es `{"finalGrade":5.5}`. La respuesta indica `PASSED` desde 4,0 o `FAILED` bajo 4,0.
- `GET /api/student/academic-history` muestra el historial propio del estudiante. `GET /api/admin/students/{id}/academic-history` permite consultarlo como administrador.

Para cerrar un período, el administrador usa `PUT /api/admin/periods/{id}/status` con `{"status":"CLOSED"}`. El cierre falla con `409 Conflict` y enumera las secciones con inscripciones activas sin calificación. Si todas tienen nota, los resultados quedan en el historial, un estudiante que reprueba por segunda vez una misma asignatura pasa a `DISMISSED` y quien aprueba todo su plan pasa a `GRADUATED`. El cierre es irreversible: las notas de un período cerrado no se pueden cambiar y el período no se puede reabrir. Las inscripciones anuladas no necesitan nota.

Las solicitudes y respuestas están documentadas en Swagger (`http://localhost:8080/swagger-ui.html`). La persona que pruebe estos endpoints debe asignar `ADMIN` al usuario Keycloak y autorizarse con OAuth2 en Swagger o enviar el JWT como `Authorization: Bearer <token>`.

La administración de carreras está en `/api/admin/careers`: permite listar y consultar carreras, crear una nueva (queda `ACTIVE` y dura cuatro semestres), actualizar sus datos y cambiar su estado con `PUT /api/admin/careers/{id}/status`, enviando `{"status":"INACTIVE"}` o `{"status":"ACTIVE"}`. No existe eliminación física porque la regla del enunciado prohíbe borrar carreras con planes o estudiantes asociados.

Para detener los servicios locales sin borrar la base:

```bash
docker compose --env-file infra/keycloak/.env -f infra/keycloak/docker-compose.yml down
```

Los datos persisten en un volumen. `down -v` elimina también ese volumen y borra los datos locales de Keycloak.

## Documentación para estudiar y presentar

Consultar [`docs/guia-presentacion.md`](docs/guia-presentacion.md) para entender las capas, el modelo, el flujo de una solicitud, las decisiones y el orden propuesto para implementar las épicas.

## Pruebas backend

Desde `backend/`, ejecutar `mvn verify`. Esto ejecuta las pruebas unitarias y de integración, genera el reporte JaCoCo en `target/site/jacoco/index.html` y falla si la cobertura de líneas del paquete de servicios cae por debajo del 90%. En la última ejecución pasaron 119 pruebas y la cobertura fue de 90,03% (641 de 712 líneas). La matriz de 18 escenarios de integración está resumida en la guía de presentación.

## CD y despliegue en Docker Compose

El workflow `.github/workflows/cd.yml` prueba el backend con `mvn verify`, compila el frontend y solo publica imágenes en Docker Hub después de que ambas verificaciones pasan en `main`. Para este proyecto, el único repositorio es `benjamingoycolea/siga-ipt`; `tagname` en una dirección Docker Hub es un marcador para la etiqueta, no parte del nombre del repositorio. El workflow publica:

- `benjamingoycolea/siga-ipt:backend-latest` y `backend-<sha>`
- `benjamingoycolea/siga-ipt:frontend-latest` y `frontend-<sha>`

Para habilitar la publicación:

1. En Docker Hub, abre **Account Settings → Personal access tokens**, crea un token con permiso **Read & Write** y cópialo. No uses tu contraseña de Docker Hub.
2. En el repositorio de GitHub, abre **Settings → Secrets and variables → Actions**. Crea estos **Repository secrets**: `DOCKERHUB_USERNAME` = `benjamingoycolea`, `DOCKERHUB_TOKEN` = el token creado en Docker Hub.
3. En la pestaña **Variables**, crea `VITE_KEYCLOAK_URL` con la URL pública real de Keycloak, `VITE_KEYCLOAK_REALM` = `siga-ipt` y `VITE_KEYCLOAK_CLIENT_ID` = `siga-ipt-web`. No uses `localhost` para la imagen de producción; el cliente público de Keycloak también debe permitir el dominio final del frontend en sus redirect URIs y web origins.
4. Sube el código al repositorio de GitHub en la rama `main`. El workflow aparecerá en **Actions**: primero ejecutará `mvn verify` y el build frontend; solo si pasan, iniciará sesión y publicará las cuatro etiquetas en el mismo repositorio Docker Hub.

Para desplegar, configura primero un realm de Keycloak accesible desde el navegador y desde los contenedores; el cliente público debe permitir como redirect URI y web origin el dominio del frontend. En el servidor, copia `infra/production/.env.example` a `.env`, completa valores propios y ejecuta. `IMAGE_TAG=latest` usa la etiqueta latest de ambos servicios; para fijar una misma versión de commit, asigna su SHA a `IMAGE_TAG`:

```bash
docker compose --env-file .env -f docker-compose.yml pull
docker compose --env-file .env -f docker-compose.yml up -d --scale backend=3
```

Ejecuta ambos comandos desde `infra/production/`. Nginx publica el puerto 80, sirve el frontend a través del contenedor Caddy y distribuye `/api` entre las tres réplicas; PostgreSQL conserva los datos en un volumen. Configura TLS en el proxy o balanceador HTTPS del proveedor de nube y no subas el `.env` al repositorio. La imagen backend crea/actualiza el esquema y carga los catálogos académicos iniciales al iniciar por primera vez; este mecanismo simplificado es apropiado para la entrega académica, pero puede reemplazarse por migraciones versionadas si el sistema evoluciona.
