package com.ns.selai.testmanagement.service;

import com.ns.selai.testmanagement.dto.ProjectDTO;
import com.ns.selai.testmanagement.exception.ResourceNotFoundException;
import com.ns.selai.testmanagement.model.Project;
import com.ns.selai.testmanagement.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;

    /**
     * Create a new project
     */
    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        log.info("Creating new project: {}", projectDTO.getName());

        Project project = Project.builder()
                .name(projectDTO.getName())
                .url(projectDTO.getUrl())
                .description(projectDTO.getDescription())
                .createdBy(projectDTO.getCreatedBy())
                .browserType(projectDTO.getBrowserType() != null ? projectDTO.getBrowserType() : "chrome")
                .testType(projectDTO.getTestType() != null ? projectDTO.getTestType() : "smoke")
                .isActive(true)
                .build();

        Project savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return convertToDTO(savedProject);
    }

    /**
     * Get all active projects
     */
    public List<ProjectDTO> getAllProjects() {
        log.info("Fetching all active projects");
        return projectRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get project by ID
     */
    public ProjectDTO getProjectById(Long id) {
        log.info("Fetching project with ID: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));
        return convertToDTO(project);
    }

    /**
     * Update project
     */
    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        // Update fields
        if (projectDTO.getName() != null) {
            project.setName(projectDTO.getName());
        }
        if (projectDTO.getUrl() != null) {
            project.setUrl(projectDTO.getUrl());
        }
        if (projectDTO.getDescription() != null) {
            project.setDescription(projectDTO.getDescription());
        }
        if (projectDTO.getBrowserType() != null) {
            project.setBrowserType(projectDTO.getBrowserType());
        }
        if (projectDTO.getTestType() != null) {
            project.setTestType(projectDTO.getTestType());
        }

        Project updatedProject = projectRepository.save(project);
        log.info("Project updated successfully: {}", updatedProject.getId());

        return convertToDTO(updatedProject);
    }

    /**
     * Delete project (soft delete)
     */
    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        project.setIsActive(false);
        projectRepository.save(project);

        log.info("Project soft deleted successfully: {}", id);
    }

    /**
     * Search projects by name
     */
    public List<ProjectDTO> searchProjects(String searchTerm) {
        log.info("Searching projects with term: {}", searchTerm);
        return projectRepository.searchByName(searchTerm)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get projects by test type
     */
    public List<ProjectDTO> getProjectsByTestType(String testType) {
        log.info("Fetching projects with test type: {}", testType);
        return projectRepository.findByTestType(testType)
                .stream()
                .filter(Project::getIsActive)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Project entity to DTO
     */
    private ProjectDTO convertToDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .url(project.getUrl())
                .description(project.getDescription())
                .createdBy(project.getCreatedBy())
                .browserType(project.getBrowserType())
                .testType(project.getTestType())
                .createdAt(project.getCreatedAt() != null ? project.getCreatedAt().toString() : null)
                .updatedAt(project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null)
                .isActive(project.getIsActive())
                .build();
    }
}
