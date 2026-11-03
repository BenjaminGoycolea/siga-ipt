BEGIN;

INSERT INTO academic_periods (academic_year, semester, status)
VALUES (2025, 1, 'CLOSED'), (2026, 2, 'OPEN')
ON CONFLICT (academic_year, semester) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM academic_periods
        WHERE academic_year = 2025 AND semester = 1 AND status = 'CLOSED'
    ) THEN
        RAISE EXCEPTION 'Demo period 2025-1 already exists but is not CLOSED; no demo data was loaded.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM academic_periods
        WHERE academic_year = 2026 AND semester = 2 AND status = 'OPEN'
    ) THEN
        RAISE EXCEPTION 'Demo period 2026-2 already exists but is not OPEN; no demo data was loaded.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM careers career
        JOIN study_plans plan ON plan.career_id = career.id AND plan.current_plan = true
        JOIN courses course ON course.study_plan_id = plan.id
        WHERE career.code = '10450'
          AND plan.version = '2021.3'
          AND course.code IN ('TAP101', 'TAP102', 'TAP103', 'TAP104', 'TAP201', 'TAP202', 'TAP204', 'TAP205')
        GROUP BY plan.id
        HAVING COUNT(DISTINCT course.code) = 8
    ) THEN
        RAISE EXCEPTION 'The current TAP plan or required demo courses are missing; no demo data was loaded.';
    END IF;
END $$;

INSERT INTO teachers (
    national_id, full_name, email, professional_title, academic_degree, status
)
VALUES
    ('DEMO-T-001', 'Docente Demo Uno', 'docente1@example.invalid', 'Ingeniería en Informática', 'Magíster en Tecnología', 'ACTIVE'),
    ('DEMO-T-002', 'Docente Demo Dos', 'docente2@example.invalid', 'Ingeniería de Software', 'Magíster en Ciencias', 'ACTIVE'),
    ('DEMO-T-003', 'Docente Demo Tres', 'docente3@example.invalid', 'Analista Programador', 'Licenciatura en Informática', 'ACTIVE')
ON CONFLICT (national_id) DO NOTHING;

DO $$
DECLARE
    has_section_mismatch boolean;
BEGIN
    WITH demo_sections(academic_year, semester, course_code, teacher_code, schedule_blocks) AS (
        VALUES
            (2025, 1, 'TAP101', 'DEMO-T-001', ARRAY['L1', 'M1', 'W1']::varchar[]),
            (2025, 1, 'TAP102', 'DEMO-T-002', ARRAY['L2', 'M2']::varchar[]),
            (2025, 1, 'TAP103', 'DEMO-T-003', ARRAY['L3', 'M3']::varchar[]),
            (2025, 1, 'TAP104', 'DEMO-T-001', ARRAY['J1', 'V1']::varchar[]),
            (2026, 2, 'TAP201', 'DEMO-T-001', ARRAY['L2', 'M2', 'W2']::varchar[]),
            (2026, 2, 'TAP202', 'DEMO-T-002', ARRAY['L3', 'M3', 'W3']::varchar[]),
            (2026, 2, 'TAP204', 'DEMO-T-003', ARRAY['J3', 'V3', 'L4']::varchar[]),
            (2026, 2, 'TAP205', 'DEMO-T-001', ARRAY['M4', 'W4']::varchar[])
    )
    SELECT EXISTS (
        SELECT 1
        FROM demo_sections demo
        JOIN academic_periods period
            ON period.academic_year = demo.academic_year
            AND period.semester = demo.semester
        JOIN careers career ON career.code = '10450'
        JOIN study_plans plan
            ON plan.career_id = career.id
            AND plan.version = '2021.3'
        JOIN courses course
            ON course.study_plan_id = plan.id
            AND course.code = demo.course_code
        JOIN sections section
            ON section.course_id = course.id
            AND section.academic_period_id = period.id
        JOIN teachers teacher ON teacher.national_id = demo.teacher_code
        WHERE section.teacher_id <> teacher.id
           OR COALESCE(
                (
                    SELECT array_agg(existing_block.schedule_block ORDER BY existing_block.schedule_block)
                    FROM section_schedule_blocks existing_block
                    WHERE existing_block.section_id = section.id
                ),
                ARRAY[]::varchar[]
           ) <> (
                SELECT array_agg(expected_block.schedule_block ORDER BY expected_block.schedule_block)
                FROM unnest(demo.schedule_blocks) AS expected_block(schedule_block)
           )
    ) INTO has_section_mismatch;

    IF has_section_mismatch THEN
        RAISE EXCEPTION 'A demo course section already exists with different assignment or schedule; no demo data was loaded.';
    END IF;
END $$;

INSERT INTO students (
    national_id, full_name, email, status, career_id, study_plan_id
)
SELECT demo.national_id, demo.full_name, demo.email, 'REGULAR', career.id, plan.id
FROM (VALUES
    ('DEMO-S-001', 'Estudiante Demo Uno', 'estudiante1@example.invalid'),
    ('DEMO-S-002', 'Estudiante Demo Dos', 'estudiante2@example.invalid'),
    ('DEMO-S-003', 'Estudiante Demo Tres', 'estudiante3@example.invalid'),
    ('DEMO-S-004', 'Estudiante Demo Cuatro', 'estudiante4@example.invalid'),
    ('DEMO-S-005', 'Estudiante Demo Cinco', 'estudiante5@example.invalid'),
    ('DEMO-S-006', 'Estudiante Demo Seis', 'estudiante6@example.invalid')
) AS demo(national_id, full_name, email)
JOIN careers career ON career.code = '10450'
JOIN study_plans plan ON plan.career_id = career.id
    AND plan.version = '2021.3'
    AND plan.current_plan = true
ON CONFLICT (national_id) DO NOTHING;

INSERT INTO student_matriculations (student_id, academic_period_id, matriculated)
SELECT student.id, period.id, true
FROM students student
CROSS JOIN academic_periods period
WHERE student.national_id IN (
    'DEMO-S-001', 'DEMO-S-002', 'DEMO-S-003',
    'DEMO-S-004', 'DEMO-S-005', 'DEMO-S-006'
)
  AND period.academic_year = 2026
  AND period.semester = 2
ON CONFLICT (student_id, academic_period_id) DO NOTHING;

WITH demo_sections(academic_year, semester, course_code, teacher_code, schedule_blocks) AS (
    VALUES
        (2025, 1, 'TAP101', 'DEMO-T-001', ARRAY['L1', 'M1', 'W1']::varchar[]),
        (2025, 1, 'TAP102', 'DEMO-T-002', ARRAY['L2', 'M2']::varchar[]),
        (2025, 1, 'TAP103', 'DEMO-T-003', ARRAY['L3', 'M3']::varchar[]),
        (2025, 1, 'TAP104', 'DEMO-T-001', ARRAY['J1', 'V1']::varchar[]),
        (2026, 2, 'TAP201', 'DEMO-T-001', ARRAY['L2', 'M2', 'W2']::varchar[]),
        (2026, 2, 'TAP202', 'DEMO-T-002', ARRAY['L3', 'M3', 'W3']::varchar[]),
        (2026, 2, 'TAP204', 'DEMO-T-003', ARRAY['J3', 'V3', 'L4']::varchar[]),
        (2026, 2, 'TAP205', 'DEMO-T-001', ARRAY['M4', 'W4']::varchar[])
)
INSERT INTO sections (course_id, academic_period_id, teacher_id, capacity)
SELECT course.id, period.id, teacher.id, 30
FROM demo_sections demo
JOIN academic_periods period
    ON period.academic_year = demo.academic_year
    AND period.semester = demo.semester
JOIN careers career ON career.code = '10450'
JOIN study_plans plan
    ON plan.career_id = career.id
    AND plan.version = '2021.3'
JOIN courses course
    ON course.study_plan_id = plan.id
    AND course.code = demo.course_code
JOIN teachers teacher ON teacher.national_id = demo.teacher_code
ON CONFLICT (course_id, academic_period_id) DO NOTHING;

WITH demo_sections(academic_year, semester, course_code, schedule_blocks) AS (
    VALUES
        (2025, 1, 'TAP101', ARRAY['L1', 'M1', 'W1']::varchar[]),
        (2025, 1, 'TAP102', ARRAY['L2', 'M2']::varchar[]),
        (2025, 1, 'TAP103', ARRAY['L3', 'M3']::varchar[]),
        (2025, 1, 'TAP104', ARRAY['J1', 'V1']::varchar[]),
        (2026, 2, 'TAP201', ARRAY['L2', 'M2', 'W2']::varchar[]),
        (2026, 2, 'TAP202', ARRAY['L3', 'M3', 'W3']::varchar[]),
        (2026, 2, 'TAP204', ARRAY['J3', 'V3', 'L4']::varchar[]),
        (2026, 2, 'TAP205', ARRAY['M4', 'W4']::varchar[])
)
INSERT INTO section_schedule_blocks (section_id, schedule_block)
SELECT section.id, block.schedule_block
FROM demo_sections demo
JOIN academic_periods period
    ON period.academic_year = demo.academic_year
    AND period.semester = demo.semester
JOIN careers career ON career.code = '10450'
JOIN study_plans plan
    ON plan.career_id = career.id
    AND plan.version = '2021.3'
JOIN courses course
    ON course.study_plan_id = plan.id
    AND course.code = demo.course_code
JOIN sections section
    ON section.course_id = course.id
    AND section.academic_period_id = period.id
CROSS JOIN LATERAL unnest(demo.schedule_blocks) AS block(schedule_block)
ON CONFLICT DO NOTHING;

WITH demo_grades(student_code, course_code, final_grade) AS (
    VALUES
        ('DEMO-S-001', 'TAP101', 6.2), ('DEMO-S-001', 'TAP102', 5.8), ('DEMO-S-001', 'TAP103', 6.5), ('DEMO-S-001', 'TAP104', 5.9),
        ('DEMO-S-002', 'TAP101', 5.4), ('DEMO-S-002', 'TAP102', 6.1), ('DEMO-S-002', 'TAP103', 5.7), ('DEMO-S-002', 'TAP104', 6.3),
        ('DEMO-S-003', 'TAP101', 6.7), ('DEMO-S-003', 'TAP102', 5.2), ('DEMO-S-003', 'TAP103', 6.0), ('DEMO-S-003', 'TAP104', 6.4),
        ('DEMO-S-004', 'TAP101', 5.9), ('DEMO-S-004', 'TAP102', 6.5), ('DEMO-S-004', 'TAP103', 5.3), ('DEMO-S-004', 'TAP104', 6.1),
        ('DEMO-S-005', 'TAP101', 6.4), ('DEMO-S-005', 'TAP102', 5.6), ('DEMO-S-005', 'TAP103', 6.2), ('DEMO-S-005', 'TAP104', 5.5),
        ('DEMO-S-006', 'TAP101', 5.1), ('DEMO-S-006', 'TAP102', 6.3), ('DEMO-S-006', 'TAP103', 5.8), ('DEMO-S-006', 'TAP104', 6.6)
)
INSERT INTO course_enrollments (
    student_id, course_id, section_id, academic_period_id, registered_at, status, final_grade
)
SELECT student.id, course.id, section.id, period.id, NOW(), 'ENROLLED', demo.final_grade
FROM demo_grades demo
JOIN students student ON student.national_id = demo.student_code
JOIN academic_periods period ON period.academic_year = 2025 AND period.semester = 1
JOIN careers career ON career.code = '10450'
JOIN study_plans plan ON plan.career_id = career.id AND plan.version = '2021.3'
JOIN courses course ON course.study_plan_id = plan.id AND course.code = demo.course_code
JOIN sections section ON section.course_id = course.id AND section.academic_period_id = period.id
ON CONFLICT (student_id, course_id, academic_period_id) DO NOTHING;

WITH demo_students(student_code) AS (
    VALUES
        ('DEMO-S-001'), ('DEMO-S-002'), ('DEMO-S-003'),
        ('DEMO-S-004'), ('DEMO-S-005'), ('DEMO-S-006')
),
demo_courses(course_code) AS (
    VALUES ('TAP201'), ('TAP202'), ('TAP204'), ('TAP205')
)
INSERT INTO course_enrollments (
    student_id, course_id, section_id, academic_period_id, registered_at, status, final_grade
)
SELECT student.id, course.id, section.id, period.id, NOW(), 'ENROLLED', NULL
FROM demo_students demo_student
JOIN students student ON student.national_id = demo_student.student_code
CROSS JOIN demo_courses demo_course
JOIN academic_periods period ON period.academic_year = 2026 AND period.semester = 2
JOIN careers career ON career.code = '10450'
JOIN study_plans plan ON plan.career_id = career.id AND plan.version = '2021.3'
JOIN courses course ON course.study_plan_id = plan.id AND course.code = demo_course.course_code
JOIN sections section ON section.course_id = course.id AND section.academic_period_id = period.id
ON CONFLICT (student_id, course_id, academic_period_id) DO NOTHING;

COMMIT;
