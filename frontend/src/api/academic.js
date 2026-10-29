import api from './client'

export const getAcademicPeriods = () => api.get('/academic-periods').then(({ data }) => data)

export const getStudentOfferings = (periodId) =>
  api.get(`/student/periods/${periodId}/offerings`).then(({ data }) => data)

export const getStudentEnrollments = (periodId) =>
  api.get(`/student/periods/${periodId}/enrollments`).then(({ data }) => data)

export const saveStudentEnrollments = (periodId, sectionIds) =>
  api.put(`/student/periods/${periodId}/enrollments`, { sectionIds }).then(({ data }) => data)

export const cancelStudentEnrollment = (periodId, enrollmentId) =>
  api.delete(`/student/periods/${periodId}/enrollments/${enrollmentId}`).then(({ data }) => data)

export const getAcademicHistory = () =>
  api.get('/student/academic-history').then(({ data }) => data)

export const getTeacherSections = (periodId) =>
  api.get('/teacher/sections', { params: { periodId } }).then(({ data }) => data)

export const getSectionEnrollments = (sectionId) =>
  api.get(`/teacher/sections/${sectionId}/enrollments`).then(({ data }) => data)

export const saveFinalGrade = (sectionId, enrollmentId, finalGrade) =>
  api.put(`/teacher/sections/${sectionId}/enrollments/${enrollmentId}/grade`, { finalGrade })
    .then(({ data }) => data)

export function getErrorMessage(error) {
  return error.response?.data?.detail
    || error.response?.data?.message
    || error.message
    || 'Ocurrió un error inesperado.'
}
