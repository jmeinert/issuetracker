package de.jmeinert.issuetracker.issue;

import de.jmeinert.issuetracker.auth.AuthTestHelper;
import de.jmeinert.issuetracker.config.TestcontainersConfiguration;
import de.jmeinert.issuetracker.project.Project;
import de.jmeinert.issuetracker.project.ProjectRepository;
import de.jmeinert.issuetracker.project.ProjectResponse;
import de.jmeinert.issuetracker.user.User;
import de.jmeinert.issuetracker.user.UserRepository;
import de.jmeinert.issuetracker.user.UserRole;
import de.jmeinert.issuetracker.user.UserTestBuilder;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IssueIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    private Project project;

    private User reporter;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        project = new Project("TestName", "TestDescription");
        projectRepository.saveAndFlush(project);

        reporter = new UserTestBuilder()
            .username("reporter")
            .email("reporter@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .enabled(true)
            .build();
        userRepository.saveAndFlush(reporter);

        auth = new AuthTestHelper(mockMvc, jsonMapper);
    }

    @Test
    void create_persistsIssueWithAuthenticatedUserAsReporter() throws Exception {
        String token = auth.login("reporter", "password");

        mockMvc.perform(post("/api/projects/{projectId}/issues", project.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reporterId").value(reporter.getId()));

        entityManager.flush();
        entityManager.clear();

        var issues = issueRepository.findAll();
        assertEquals(1, issues.size());

        Issue issue = issues.getFirst();

        assertEquals("TestTitle", issue.getTitle());
        assertEquals("TestDescription", issue.getDescription());
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertEquals(IssuePriority.LOW, issue.getPriority());
        assertEquals(project.getId(), issue.getProject().getId());
        assertEquals(reporter.getId(), issue.getReporter().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignmentLifecycle_adminAssignsReassignsAndUnassignsIssue() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        Long issueId = issue.getId();

        User assignee1 = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .enabled(true)
            .build();
        User assignee2 = new UserTestBuilder()
            .username("assignee2")
            .email("assignee2@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .enabled(true)
            .build();
        userRepository.saveAllAndFlush(List.of(assignee1, assignee2));

        entityManager.clear();
        Issue persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertNull(persistedIssue.getAssignee());

        // Assign issue
        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assignee1.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(assignee1.getId()));

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertEquals(assignee1.getId(), persistedIssue.getAssignee().getId());

        // Reassign issue
        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assignee2.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").value(assignee2.getId()));

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertEquals(assignee2.getId(), persistedIssue.getAssignee().getId());

        // Unassign issue
        mockMvc.perform(delete("/api/issues/{issueId}/assignee", issueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assigneeId").isEmpty());

        entityManager.flush();
        entityManager.clear();
        persistedIssue = issueRepository.findById(issueId).orElseThrow();
        assertNull(persistedIssue.getAssignee());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200_whenPrincipalIsAnAdmin() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        mockMvc.perform(put("/api/issues/{issueId}", issue.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("UpdatedTestTitle"))
            .andExpect(jsonPath("$.description").value("UpdatedTestDescription"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void update_returns200_whenPrincipalIsTheReporter() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        String token = auth.login("reporter", "password");

        mockMvc.perform(put("/api/issues/{issueId}", issue.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("UpdatedTestTitle"))
            .andExpect(jsonPath("$.description").value("UpdatedTestDescription"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void update_returns200_whenPrincipalIsTheAssignee() throws Exception {
        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .enabled(true)
            .build();
        userRepository.saveAndFlush(assignee);

        Issue issue = new IssueTestBuilder(project, reporter)
            .assignee(assignee)
            .build();
        issueRepository.saveAndFlush(issue);

        String token = auth.login("assignee", "password");

        mockMvc.perform(put("/api/issues/{issueId}", issue.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("UpdatedTestTitle"))
            .andExpect(jsonPath("$.description").value("UpdatedTestDescription"))
            .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void update_returns403_whenPrincipalIsNotAParticipant() throws Exception {
        User user = new UserTestBuilder()
            .username("user")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .enabled(true)
            .build();
        userRepository.saveAndFlush(user);

        Issue issue = new IssueTestBuilder(project, reporter)
            .title("TestTitle")
            .description("TestDescription")
            .priority(IssuePriority.LOW)
            .build();
        issueRepository.saveAndFlush(issue);

        Long issueId = issue.getId();

        String token = auth.login("user", "password");

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));

        entityManager.flush();
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issueId).orElseThrow();

        assertEquals("TestTitle", persistedIssue.getTitle());
        assertEquals("TestDescription", persistedIssue.getDescription());
        assertEquals(IssuePriority.LOW, persistedIssue.getPriority());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_returns200_whenPrincipalIsAnAdmin() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        mockMvc.perform(patch("/api/issues/{issueId}/status", issue.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_returns200_whenPrincipalIsTheReporter() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter).build();
        issueRepository.saveAndFlush(issue);

        String token = auth.login("reporter", "password");

        mockMvc.perform(patch("/api/issues/{issueId}/status", issue.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_returns200_whenPrincipalIsTheAssignee() throws Exception {
        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .enabled(true)
            .build();
        userRepository.saveAndFlush(assignee);

        Issue issue = new IssueTestBuilder(project, reporter)
            .assignee(assignee)
            .build();
        issueRepository.saveAndFlush(issue);

        String token = auth.login("assignee", "password");

        mockMvc.perform(patch("/api/issues/{issueId}/status", issue.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatus_returns403_whenPrincipalIsNotAParticipant() throws Exception {
        User user = new UserTestBuilder()
            .username("user")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .enabled(true)
            .build();
        userRepository.saveAndFlush(user);

        Issue issue = new IssueTestBuilder(project, reporter)
            .status(IssueStatus.OPEN)
            .build();
        issueRepository.saveAndFlush(issue);

        Long issueId = issue.getId();

        String token = auth.login("user", "password");

        mockMvc.perform(patch("/api/issues/{issueId}/status", issueId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "IN_PROGRESS"
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("Access denied"));

        entityManager.flush();
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issueId).orElseThrow();

        assertEquals(IssueStatus.OPEN, persistedIssue.getStatus());
    }

    @Test
    void changeStatus_returns409_whenStatusTransitionIsInvalid() throws Exception {
        Issue issue = new IssueTestBuilder(project, reporter)
            .status(IssueStatus.OPEN)
            .build();
        issueRepository.saveAndFlush(issue);

        String token = auth.login("reporter", "password");

        mockMvc.perform(patch("/api/issues/{issueId}/status", issue.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "status": "CLOSED"
                }
                """))
            .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issue.getId()).orElseThrow();

        assertEquals(IssueStatus.OPEN, persistedIssue.getStatus());
    }

    @Test
    void issueWorkflow_adminCreatesAndAssignsIssueAndAssigneeUpdatesIt() throws Exception {
        User admin = new UserTestBuilder()
            .username("admin")
            .email("admin@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.ADMIN)
            .build();
        User assignee = new UserTestBuilder()
            .username("assignee")
            .email("assignee@example.com")
            .passwordHash(passwordEncoder.encode("password"))
            .role(UserRole.USER)
            .build();
        userRepository.saveAllAndFlush(List.of(admin, assignee));

        String adminToken = auth.login("admin", "password");

        String projectResponse = mockMvc.perform(post("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "TestName",
                    "description": "TestDescription"
                }
                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long projectId = jsonMapper.readValue(projectResponse, ProjectResponse.class).id();

        String issueResponse = mockMvc.perform(post("/api/projects/{projectId}/issues", projectId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "TestTitle",
                    "description": "TestDescription",
                    "priority": "LOW"
                }
                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long issueId = jsonMapper.readValue(issueResponse, IssueResponse.class).id();

        mockMvc.perform(patch("/api/issues/{issueId}/assignee", issueId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "assigneeId": %d
                }
                """.formatted(assignee.getId())))
            .andExpect(status().isOk());

        String assigneeToken = auth.login("assignee", "password");

        mockMvc.perform(put("/api/issues/{issueId}", issueId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + assigneeToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "UpdatedTestTitle",
                    "description": "UpdatedTestDescription",
                    "priority": "MEDIUM"
                }
                """))
            .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Issue persistedIssue = issueRepository.findById(issueId).orElseThrow();

        assertEquals("UpdatedTestTitle", persistedIssue.getTitle());
        assertEquals("UpdatedTestDescription", persistedIssue.getDescription());
        assertEquals(IssuePriority.MEDIUM, persistedIssue.getPriority());
        assertEquals(projectId, persistedIssue.getProject().getId());
        assertEquals(admin.getId(), persistedIssue.getReporter().getId());
        assertEquals(assignee.getId(), persistedIssue.getAssignee().getId());
    }
}
