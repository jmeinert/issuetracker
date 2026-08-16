package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PersistenceConfig.class})
class IssueRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllByProject_returnsPageOfIssuesOfGivenProject() {
        Project project1 = new Project("TestName", "TestDescription");
        Project project2 = new Project("TestName2", "TestDescription2");

        Issue issue1 = new Issue(
            "Charlie",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project1
        );
        Issue issue2 = new Issue(
            "Alpha",
            "TestDescription2",
            IssueStatus.IN_PROGRESS,
            IssuePriority.MEDIUM,
            project1
        );
        Issue issue3 = new Issue(
            "Bravo",
            "TestDescription3",
            IssueStatus.RESOLVED,
            IssuePriority.HIGH,
            project1
        );
        Issue issue4 = new Issue(
            "Tango",
            "TestDescription4",
            IssueStatus.RESOLVED,
            IssuePriority.HIGH,
            project2
        );

        Pageable pageable = PageRequest.of(
            2,
            1,
            Sort.by(Sort.Order.asc("title"))
        );

        projectRepository.saveAllAndFlush(List.of(project1, project2));
        issueRepository.saveAllAndFlush(List.of(issue1, issue2, issue3, issue4));
        entityManager.clear();

        Page<Issue> issuesOfProject1 = issueRepository.findAllByProject(project1, pageable);

        assertThat(issuesOfProject1)
            .extracting(Issue::getId)
            .containsExactly(issue1.getId());
        assertThat(issuesOfProject1.getNumber())
            .isEqualTo(2);
        assertThat(issuesOfProject1.getSize())
            .isEqualTo(1);
        assertThat(issuesOfProject1.getTotalElements())
            .isEqualTo(3);
        assertThat(issuesOfProject1.getTotalPages())
            .isEqualTo(3);
        assertThat(issuesOfProject1.isFirst()).isFalse();
        assertThat(issuesOfProject1.isLast()).isTrue();
    }

    @Test
    void findAllByProject_returnsEmptyPage_whenProjectHasNoIssues() {
        Project project = new Project("TestName", "TestDescription");

        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        projectRepository.saveAndFlush(project);
        entityManager.clear();

        Page<Issue> issuesOfProject = issueRepository.findAllByProject(project, pageable);

        assertThat(issuesOfProject.getContent()).isEmpty();
        assertThat(issuesOfProject.getNumber()).isZero();
        assertThat(issuesOfProject.getSize()).isEqualTo(20);
        assertThat(issuesOfProject.getTotalElements()).isZero();
        assertThat(issuesOfProject.getTotalPages()).isZero();
        assertThat(issuesOfProject.isFirst()).isTrue();
        assertThat(issuesOfProject.isLast()).isTrue();
    }

    @Test
    void existsByProject_returnsTrue_whenProjectHasIssues() {
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

        projectRepository.saveAndFlush(project);
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        assertThat(issueRepository.existsByProject(project)).isTrue();
    }

    @Test
    void existsByProject_returnsFalse_whenProjectHasNoIssues() {
        Project project = new Project("TestName", "TestDescription");

        projectRepository.saveAndFlush(project);
        entityManager.clear();

        assertThat(issueRepository.existsByProject(project)).isFalse();
    }

    @Test
    void save_persistsIssue() {
        Project project = new Project("TestName", "TestDescription");
        projectRepository.saveAndFlush(project);

        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        issueRepository.saveAndFlush(issue);

        Long projectId = project.getId();
        Long issueId = issue.getId();

        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issueId)
            .orElseThrow();

        assertThat(persistedIssue.getId()).isEqualTo(issueId);
        assertThat(persistedIssue.getTitle()).isEqualTo("TestTitle");
        assertThat(persistedIssue.getDescription()).isEqualTo("TestDescription");
        assertThat(persistedIssue.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(persistedIssue.getPriority()).isEqualTo(IssuePriority.LOW);
        assertThat(persistedIssue.getProject().getId()).isEqualTo(projectId);
        assertThat(persistedIssue.getCreatedAt()).isNotNull();
        assertThat(persistedIssue.getUpdatedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(IssueStatus.class)
    void save_acceptsEveryIssueStatus(IssueStatus status) {
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            status,
            IssuePriority.LOW,
            project
        );

        projectRepository.saveAndFlush(project);
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issue.getId())
            .orElseThrow();

        assertThat(persistedIssue.getStatus())
            .isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(IssuePriority.class)
    void save_acceptsEveryIssuePriority(IssuePriority priority) {
        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            priority,
            project
        );

        projectRepository.saveAndFlush(project);
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issue.getId())
            .orElseThrow();

        assertThat(persistedIssue.getPriority())
            .isEqualTo(priority);
    }

    @Test
    void save_acceptsMaximumFieldLengths() {
        String title = "a".repeat(150);
        String description = "a".repeat(1000);

        Project project = new Project("TestName", "TestDescription");
        Issue issue = new Issue(
            title,
            description,
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );

        projectRepository.saveAndFlush(project);
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issue.getId())
            .orElseThrow();

        assertThat(persistedIssue.getTitle())
            .isEqualTo(title);
        assertThat(persistedIssue.getDescription())
            .isEqualTo(description);
    }
}
