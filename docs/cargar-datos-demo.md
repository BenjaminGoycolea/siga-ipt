# Cargar datos ficticios para la demo

El script `infra/production/demo-data.sql` agrega, sin borrar datos existentes:

- 6 estudiantes y 3 docentes ficticios del plan TAP.
- Un período cerrado `2025-1` con historial y notas.
- Un período abierto `2026-2` con matrícula, secciones asignadas e inscripciones sin nota final.

La carga es idempotente: se puede ejecutar otra vez sin duplicar estos registros. Si alguno de esos períodos existe con un estado distinto, el script aborta para no cambiarlo.

Desde la terminal de EC2, actualiza el repositorio y ejecuta el script dentro del contenedor PostgreSQL:

```bash
cd ~/siga-ipt
git pull
cd infra/production
sudo docker compose --env-file .env exec -T database sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < demo-data.sql
```

Tras la carga, revisa los estudiantes, docentes, secciones e inscripciones desde el portal de administración.

Los registros académicos quedan sin vincular a cuentas de Keycloak (`keycloak_subject` permanece vacío). Para entrar como estudiante o docente, crea usuarios ficticios en el realm `siga-ipt`, asígnales el rol `STUDENT` o `TEACHER` y vincula cada cuenta a su ficha desde el portal de administración. El portal solicita el `sub` de Keycloak; cada persona puede obtenerlo de su propio token o desde el ID del usuario en la consola de Keycloak. No uses datos personales reales ni pongas contraseñas en este repositorio.
