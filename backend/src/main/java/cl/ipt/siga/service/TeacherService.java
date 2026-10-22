package cl.ipt.siga.service;

import cl.ipt.siga.dto.TeacherRequest;
import cl.ipt.siga.dto.TeacherResponse;
import cl.ipt.siga.entity.Teacher;
import cl.ipt.siga.entity.enums.AcademicPeriodStatus;
import cl.ipt.siga.entity.enums.TeacherStatus;
import cl.ipt.siga.exception.BusinessConflictException;
import cl.ipt.siga.exception.ResourceNotFoundException;
import cl.ipt.siga.repository.SectionRepository;
import cl.ipt.siga.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

    public TeacherService(TeacherRepository teacherRepository, SectionRepository sectionRepository) {
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll(TeacherStatus status) {
        List<Teacher> teachers = status == null
                ? teacherRepository.findAllByOrderByFullNameAsc()
                : teacherRepository.findByStatusOrderByFullNameAsc(status);
        return teachers.stream().map(TeacherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getTeacher(id));
    }

    public TeacherResponse create(TeacherRequest request) {
        String nationalId = normalizeNationalId(request.nationalId());
        String email = normalizeEmail(request.email());
        ensureNationalIdAvailable(nationalId, null);
        ensureEmailAvailable(email, null);

        Teacher teacher = new Teacher();
        applyRequest(teacher, request, nationalId, email);
        teacher.setStatus(TeacherStatus.ACTIVE);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = getTeacher(id);
        String nationalId = normalizeNationalId(request.nationalId());
        String email = normalizeEmail(request.email());
        ensureNationalIdAvailable(nationalId, id);
        ensureEmailAvailable(email, id);

        applyRequest(teacher, request, nationalId, email);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse updateStatus(Long id, TeacherStatus status) {
        Teacher teacher = getTeacher(id);
        if (status == TeacherStatus.INACTIVE
                && sectionRepository.existsByTeacherIdAndAcademicPeriod_Status(
                id,
                AcademicPeriodStatus.OPEN
        )) {
            throw new BusinessConflictException(
                    "A teacher assigned to a section in an open academic period cannot be made inactive."
            );
        }
        teacher.setStatus(status);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public TeacherResponse updateKeycloakSubject(Long id, String keycloakSubject) {
        Teacher teacher = teacherRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + id));
        String subject = keycloakSubject.trim();
        teacherRepository.findByKeycloakSubject(subject)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("This Keycloak account is already linked to another teacher.");
                });
        teacher.setKeycloakSubject(subject);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    private void applyRequest(Teacher teacher, TeacherRequest request, String nationalId, String email) {
        teacher.setNationalId(nationalId);
        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(email);
        teacher.setProfessionalTitle(request.professionalTitle().trim());
        teacher.setAcademicDegree(request.academicDegree().trim());
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + id));
    }

    private void ensureNationalIdAvailable(String nationalId, Long excludedTeacherId) {
        teacherRepository.findByNationalId(nationalId)
                .filter(existing -> !existing.getId().equals(excludedTeacherId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A teacher with this national ID already exists.");
                });
    }

    private void ensureEmailAvailable(String email, Long excludedTeacherId) {
        teacherRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(excludedTeacherId))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("A teacher with this email already exists.");
                });
    }

    private String normalizeNationalId(String nationalId) {
        return nationalId.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
