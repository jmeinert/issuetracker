package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.config.PersistenceConfig;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;
import de.jmeinert.issuetracker.project.ProjectService;

import jakarta.persistence.EntityManager;
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
    IssueService.class
})
class IssueQueryIntegrationTest {

    @MockitoBean
    private ProjectService projectService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAll_filtersIssuesByProjectId() {
        Project project1 = new Project("TestName", "TestDescription");
        Project project2 = new Project("TestName2", "TestDescription2");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project1
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project2
        );

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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.IN_PROGRESS,
            IssuePriority.LOW,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "100%",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "100",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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
        Project project = new Project("TestName", "TestDescription");

        Issue issue1 = new Issue(
            "TestTitle",
            "Description",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project
        );
        Issue issue2 = new Issue(
            "Title2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project
        );

        projectRepository.saveAndFlush(project);
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

        Issue issue1 = new Issue(
            "TestTitle",
            "TestDescription",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project1
        );
        Issue issue2 = new Issue(
            "TestTitle2",
            "TestDescription2",
            IssueStatus.OPEN,
            IssuePriority.LOW,
            project1
        );
        Issue issue3 = new Issue(
            "TestTitle3",
            "TestDescription3",
            IssueStatus.IN_PROGRESS,
            IssuePriority.MEDIUM,
            project1
        );
        Issue issue4 = new Issue(
            "TestTitle4",
            "TestDescription4",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project2
        );
        Issue issue5 = new Issue(
            "Title5",
            "Description5",
            IssueStatus.OPEN,
            IssuePriority.MEDIUM,
            project1
        );

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
