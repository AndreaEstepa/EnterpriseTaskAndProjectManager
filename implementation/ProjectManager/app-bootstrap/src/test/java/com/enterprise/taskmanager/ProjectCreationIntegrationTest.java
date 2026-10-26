package com.enterprise.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProjectCreationIntegrationTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient patchHttpClient = HttpClient.newHttpClient();

    @Test
    void registersUserCreatesOrganizationAndPersistsProjectThroughHttp() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "project-integration-" + suffix + "@example.test";
        String password = "Secure123!";
        String organizationName = "Integration " + suffix;
        String projectName = "Project " + suffix;
        UUID organizationId = null;

        try {
            ResponseEntity<String> unauthenticatedResponse = http.postForEntity(
                    "/api/v1/projects",
                    json("""
                            {
                              "organizationId": "%s",
                              "name": "Unauthorized project"
                            }
                            """.formatted(UUID.randomUUID())),
                    String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, unauthenticatedResponse.getStatusCode());

            ResponseEntity<String> registration = http.postForEntity(
                    "/api/v1/auth/register",
                    json("""
                            {
                              "email": "%s",
                              "password": "%s",
                              "fullName": "Project Integration User"
                            }
                            """.formatted(email, password)),
                    String.class);
            assertEquals(HttpStatus.CREATED, registration.getStatusCode());

            ResponseEntity<String> login = http.postForEntity(
                    "/api/v1/auth/login",
                    json("""
                            {
                              "email": "%s",
                              "password": "%s"
                            }
                            """.formatted(email, password)),
                    String.class);
            assertEquals(HttpStatus.OK, login.getStatusCode());
            String accessToken = objectMapper.readTree(login.getBody()).path("accessToken").asText();
            assertFalse(accessToken.isBlank());

            HttpHeaders authenticatedHeaders = new HttpHeaders();
            authenticatedHeaders.setBearerAuth(accessToken);
            authenticatedHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> authenticated = new HttpEntity<>(
                    """
                            {"name":"%s"}
                            """.formatted(organizationName),
                            authenticatedHeaders);
            ResponseEntity<String> organizationResponse = http.exchange(
                    "/api/v1/organizations",
                    HttpMethod.POST,
                    authenticated,
                    String.class);
            assertEquals(HttpStatus.CREATED, organizationResponse.getStatusCode());
            organizationId = UUID.fromString(
                    objectMapper.readTree(organizationResponse.getBody()).path("id").asText());

            HttpHeaders projectHeaders = new HttpHeaders();
            projectHeaders.setBearerAuth(accessToken);
            projectHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> projectRequest = new HttpEntity<>(
                    """
                            {
                              "organizationId": "%s",
                              "name": "%s",
                              "description": "Created by an HTTP integration test"
                            }
                            """.formatted(organizationId, projectName),
                    projectHeaders);
            ResponseEntity<String> projectResponse = http.exchange(
                    "/api/v1/projects",
                    HttpMethod.POST,
                    projectRequest,
                    String.class);

            assertEquals(HttpStatus.CREATED, projectResponse.getStatusCode());
            JsonNode projectJson = objectMapper.readTree(projectResponse.getBody());
            assertEquals(projectName, projectJson.path("name").asText());
            assertEquals(organizationId.toString(), projectJson.path("organizationId").asText());
            assertEquals("ACTIVE", projectJson.path("status").asText());
            UUID projectId = UUID.fromString(projectJson.path("id").asText());

            Integer persistedProjects = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM projects WHERE organization_id = ? AND name = ?",
                    Integer.class,
                    organizationId,
                    projectName);
            assertEquals(1, persistedProjects);

            ResponseEntity<String> taskResponse = http.exchange(
                    "/api/v1/projects/" + projectId + "/tasks",
                    HttpMethod.POST,
                    new HttpEntity<>("{\"title\":\"Integration task\"}", projectHeaders),
                    String.class);
            assertEquals(HttpStatus.CREATED, taskResponse.getStatusCode());
            JsonNode taskJson = objectMapper.readTree(taskResponse.getBody());
            assertEquals("Integration task", taskJson.path("title").asText());
            assertEquals("BACKLOG", taskJson.path("status").asText());
            assertEquals("MEDIUM", taskJson.path("priority").asText());
            UUID taskId = UUID.fromString(taskJson.path("id").asText());
            UUID userId = UUID.fromString(taskJson.path("createdByUserId").asText());

            ResponseEntity<String> taskListResponse = http.exchange(
                    "/api/v1/projects/" + projectId + "/tasks",
                    HttpMethod.GET,
                    new HttpEntity<>(projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, taskListResponse.getStatusCode());
            assertEquals(taskId.toString(), objectMapper.readTree(taskListResponse.getBody()).get(0).path("id").asText());

            ResponseEntity<String> taskUpdateResponse = http.exchange(
                    "/api/v1/tasks/" + taskId,
                    HttpMethod.PUT,
                    new HttpEntity<>(
                            """
                                    {
                                      "title": "Updated integration task",
                                      "description": "Updated through HTTP",
                                      "priority": "HIGH"
                                    }
                                    """,
                            projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, taskUpdateResponse.getStatusCode());
            assertEquals(
                    "Updated integration task",
                    objectMapper.readTree(taskUpdateResponse.getBody()).path("title").asText());
            assertEquals("HIGH", objectMapper.readTree(taskUpdateResponse.getBody()).path("priority").asText());
            assertEquals(1, objectMapper.readTree(taskUpdateResponse.getBody()).path("version").asInt());

            HttpResponse<String> taskAssignmentResponse = patchJson(
                    "/api/v1/tasks/" + taskId + "/assignee",
                    accessToken,
                    "{\"assigneeUserId\":\"" + userId + "\"}");
            assertEquals(HttpStatus.OK.value(), taskAssignmentResponse.statusCode());
            assertEquals(
                    userId.toString(),
                    objectMapper.readTree(taskAssignmentResponse.body()).path("assigneeUserId").asText());

            HttpResponse<String> statusTransitionResponse = patchJson(
                    "/api/v1/tasks/" + taskId + "/status",
                    accessToken,
                    "{\"targetStatus\":\"TODO\"}");
            assertEquals(HttpStatus.OK.value(), statusTransitionResponse.statusCode());
            assertEquals("TODO", objectMapper.readTree(statusTransitionResponse.body()).path("status").asText());

            HttpResponse<String> invalidTransitionResponse = patchJson(
                    "/api/v1/tasks/" + taskId + "/status",
                    accessToken,
                    "{\"targetStatus\":\"DONE\"}");
            assertEquals(HttpStatus.BAD_REQUEST.value(), invalidTransitionResponse.statusCode());

            ResponseEntity<String> duplicateProjectResponse = http.exchange(
                    "/api/v1/projects",
                    HttpMethod.POST,
                    new HttpEntity<>(
                            """
                                    {
                                      "organizationId": "%s",
                                      "name": "%s"
                                    }
                                    """.formatted(organizationId, projectName.toLowerCase()),
                            projectHeaders),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, duplicateProjectResponse.getStatusCode());

            ResponseEntity<String> projectListResponse = http.exchange(
                    "/api/v1/projects?organizationId=" + organizationId,
                    HttpMethod.GET,
                    new HttpEntity<>(projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, projectListResponse.getStatusCode());
            assertEquals(
                    projectId.toString(),
                    objectMapper.readTree(projectListResponse.getBody()).get(0).path("id").asText());

            ResponseEntity<String> projectDetailResponse = http.exchange(
                    "/api/v1/projects/" + projectId,
                    HttpMethod.GET,
                    new HttpEntity<>(projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, projectDetailResponse.getStatusCode());
            assertEquals(
                    projectName,
                    objectMapper.readTree(projectDetailResponse.getBody()).path("name").asText());

            ResponseEntity<String> updatedProjectResponse = http.exchange(
                    "/api/v1/projects/" + projectId,
                    HttpMethod.PUT,
                    new HttpEntity<>(
                            """
                                    {
                                      "name": "Updated %s",
                                      "description": "Updated integration description"
                                    }
                                    """.formatted(suffix),
                            projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, updatedProjectResponse.getStatusCode());
            assertEquals(
                    "Updated " + suffix,
                    objectMapper.readTree(updatedProjectResponse.getBody()).path("name").asText());
            assertEquals(
                    "Updated integration description",
                    objectMapper.readTree(updatedProjectResponse.getBody()).path("description").asText());

            HttpResponse<String> archivedProjectResponse = patchHttpClient.send(
                    HttpRequest.newBuilder(URI.create(
                                    http.getRootUri() + "/api/v1/projects/" + projectId + "/archive"))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .method("PATCH", HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpStatus.OK.value(), archivedProjectResponse.statusCode());
            assertEquals(
                    "ARCHIVED",
                    objectMapper.readTree(archivedProjectResponse.body()).path("status").asText());

            ResponseEntity<String> archivedTaskReadResponse = http.exchange(
                    "/api/v1/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(projectHeaders),
                    String.class);
            assertEquals(HttpStatus.OK, archivedTaskReadResponse.getStatusCode());
            HttpResponse<String> archivedTaskUpdateResponse = patchJson(
                    "/api/v1/tasks/" + taskId + "/status",
                    accessToken,
                    "{\"targetStatus\":\"IN_PROGRESS\"}");
            assertEquals(HttpStatus.FORBIDDEN.value(), archivedTaskUpdateResponse.statusCode());

            Integer archivedRows = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM projects WHERE id = ? AND status = 'ARCHIVED'",
                    Integer.class,
                    projectId);
            assertEquals(1, archivedRows);
        } finally {
            jdbcTemplate.update("DELETE FROM organizations WHERE name = ?", organizationName);
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
        }
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpResponse<String> patchJson(String path, String accessToken, String body) throws Exception {
        return patchHttpClient.send(
                HttpRequest.newBuilder(URI.create(http.getRootUri() + path))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
