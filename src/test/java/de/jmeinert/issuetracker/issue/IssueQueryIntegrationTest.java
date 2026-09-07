package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;
import de.jmeinert.issuetracker.project.ProjectService;
import de.jmeinert.issuetracker.security.AuthenticatedUserProvider;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserService;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestcontainersConfiguration.class,
    PersistenceConfig.class,
    IssueService.class,
    AuthenticatedUserProvider.class
})
class IssueQueryIntegrationTest {

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    private IssueService issueService;

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
    void findAll_filtersIssuesByProjectId() {
        Project project1 = new Project("TestName", "TestDescription");
        Project project2 = new Project("TestName2", "TestDescription2");

        Issue issue1 = new IssueTestBuilder(project1, reporter).build();
        Issue issue2 = new IssueTestBuilder(project2, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();

        projectRepository.saveAllAndFlush(List.of(project1, project2));
        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(project1.getId(), null, null, null);
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredByProjectId = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredByProjectId)
            .extracting(Issue::getId)
            .containsExactly(issue1.getId());
    }

    @Test
    void findAll_filtersIssuesByStatus() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .status(IssueStatus.IN_PROGRESS)
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, IssueStatus.IN_PROGRESS, null, null);
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredByStatus = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredByStatus)
            .extracting(Issue::getId)
            .containsExactly(issue2.getId());
    }

    @Test
    void findAll_filtersIssuesByPriority() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .priority(IssuePriority.MEDIUM)
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, IssuePriority.MEDIUM, null);
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredByPriority = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredByPriority)
            .extracting(Issue::getId)
            .containsExactly(issue2.getId());
    }

    @Test
    void findAll_normalizesSearchText() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "    testTItlE2    ");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactly(issue2.getId());
    }

    @Test
    void findAll_ignoresBlankSearchText() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "  ");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactlyInAnyOrder(issue1.getId(), issue2.getId());
    }

    @Test
    void findAll_escapesSearchText() {
        Issue issue1 = new IssueTestBuilder(project, reporter)
            .title("100%")
            .build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("100")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "100%");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactly(issue1.getId());
    }

    @Test
    void findAll_searchesIssueTitle() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "TestTitle2");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactly(issue2.getId());
    }

    @Test
    void findAll_searchesIssueDescription() {
        Issue issue1 = new IssueTestBuilder(project, reporter).build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "TestDescription2");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactly(issue2.getId());
    }

    @Test
    void findAll_searchesIssueTitleOrDescription() {
        Issue issue1 = new IssueTestBuilder(project, reporter)
            .title("TestTitle")
            .description("Description")
            .build();
        Issue issue2 = new IssueTestBuilder(project, reporter)
            .title("Title2")
            .description("TestDescription2")
            .build();

        issueRepository.saveAllAndFlush(List.of(issue1, issue2));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, null, null, "Test");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesFilteredBySearchText = issueService.findAll(filter, pageable);

        assertThat(issuesFilteredBySearchText)
            .extracting(Issue::getId)
            .containsExactlyInAnyOrder(issue1.getId(), issue2.getId());
    }

    @Test
    void findAll_returnsOnlyIssuesMatchingAllFilters() {
        Project project1 = new Project("TestName", "TestDescription");
        Project project2 = new Project("TestName2", "TestDescription2");

        Issue issue1 = new IssueTestBuilder(project1, reporter)
            .priority(IssuePriority.MEDIUM)
            .build();
        Issue issue2 = new IssueTestBuilder(project1, reporter)
            .title("TestTitle2")
            .description("TestDescription2")
            .build();
        Issue issue3 = new IssueTestBuilder(project1, reporter)
            .title("TestTitle3")
            .description("TestDescription3")
            .status(IssueStatus.IN_PROGRESS)
            .priority(IssuePriority.MEDIUM)
            .build();
        Issue issue4 = new IssueTestBuilder(project2, reporter)
            .title("TestTitle4")
            .description("TestDescription4")
            .priority(IssuePriority.MEDIUM)
            .build();
        Issue issue5 = new IssueTestBuilder(project1, reporter)
            .title("Title5")
            .description("Description5")
            .priority(IssuePriority.MEDIUM)
            .build();

        projectRepository.saveAllAndFlush(List.of(project1, project2));
        issueRepository.saveAllAndFlush(List.of(issue1, issue2, issue3, issue4, issue5));
        entityManager.clear();

        IssueFilter filter = new IssueFilter(project1.getId(), IssueStatus.OPEN, IssuePriority.MEDIUM, "test");
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> issuesMatchingAllFilters = issueService.findAll(filter, pageable);

        assertThat(issuesMatchingAllFilters)
            .extracting(Issue::getId)
            .containsExactly(issue1.getId());
    }

    @Test
    void findAll_returnsEmptyPage_whenFilterYieldsNoResults() {
        Issue issue = new IssueTestBuilder(project, reporter).build();

        issueRepository.saveAndFlush(issue);
        entityManager.clear();

        IssueFilter filter = new IssueFilter(null, IssueStatus.CLOSED, null, null);
        Pageable pageable = PageRequest.of(
            0,
            20,
            Sort.by(Sort.Order.desc("createdAt"))
        );

        Page<Issue> closedIssues = issueService.findAll(filter, pageable);

        assertThat(closedIssues.getContent()).isEmpty();
        assertThat(closedIssues.getTotalElements()).isZero();
    }
}
