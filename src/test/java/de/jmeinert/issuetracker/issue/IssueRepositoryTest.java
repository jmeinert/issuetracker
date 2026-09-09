package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Project project;

    private User reporter;

    @BeforeEach
    void setUp() {
        project = new Project("TestName", "TestDescription");
        projectRepository.saveAndFlush(project);

        reporter = new UserTestBuilder()
            .username("reporter")
            .email("reporter@example.com")
            .build();
        userRepository.saveAndFlush(reporter);

        entityManager.clear();
    }

    @Test
    void findAllByProject_returnsPageOfIssuesOfGivenProject() {
        Project project1 = new Project("TestName", "TestDescription");
        Project project2 = new Project("TestName2", "TestDescription2");

        Issue issue1 = new IssueTestBuilder(project1, reporter)
            .title("Charlie")
            .build();
        Issue issue2 = new IssueTestBuilder(project1, reporter)
            .title("Alpha")
            .description("TestDescription2")
            .build();
        Issue issue3 = new IssueTestBuilder(project1, reporter)
            .title("Bravo")
            .description("TestDescription3")
            .build();
        Issue issue4 = new IssueTestBuilder(project2, reporter)
            .title("Tango")
            .description("TestDescription4")
            .build();

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
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

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
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        assertThat(issueRepository.existsByProject(project)).isTrue();
    }

    @Test
    void existsByProject_returnsFalse_whenProjectHasNoIssues() {
        assertThat(issueRepository.existsByProject(project)).isFalse();
    }

    @Test
    void existsByIdAndParticipantId_returnsTrue_whenIssueExistsAndUserIsTheReporter() {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        assertThat(issueRepository.existsByIdAndParticipantId(issue.getId(), reporter.getId()))
            .isTrue();
    }

    @Test
    void existsByIdAndParticipantId_returnsTrue_whenIssueExistsAndUserIsTheAssignee() {
        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .build();
        userRepository.saveAndFlush(assignee);

        Issue issue = new IssueTestBuilder(project, reporter)
            .assignee(assignee)
            .build();
        issueRepository.saveAndFlush(issue);

        entityManager.clear();

        assertThat(issueRepository.existsByIdAndParticipantId(issue.getId(), assignee.getId()))
            .isTrue();
    }

    @Test
    void existsByIdAndParticipantId_returnsFalse_whenIssueExistsAndUserIsNeitherReporterNorAssignee() {
        User user = new UserTestBuilder().build();
        userRepository.saveAndFlush(user);

        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .assignee(user)
            .build();
        issueRepository.saveAllAndFlush(List.of(issue1, issue2));

        entityManager.clear();

        assertThat(issueRepository.existsByIdAndParticipantId(issue1.getId(), user.getId()))
            .isFalse();
    }

    @Test
    void existsByIdAndParticipantId_returnsFalse_whenIssueDoesNotExist() {
        assertThat(issueRepository.existsByIdAndParticipantId(123L, reporter.getId()))
            .isFalse();
    }

    @Test
    void save_persistsIssue() {
        Issue issue = new IssueTestBuilder(project, reporter).build();
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
        Issue issue = new IssueTestBuilder(project, reporter)
            .status(status)
            .build();

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
        Issue issue = new IssueTestBuilder(project, reporter)
            .priority(priority)
            .build();

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

        Issue issue = new IssueTestBuilder(project, reporter)
            .title(title)
            .description(description)
            .build();

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
