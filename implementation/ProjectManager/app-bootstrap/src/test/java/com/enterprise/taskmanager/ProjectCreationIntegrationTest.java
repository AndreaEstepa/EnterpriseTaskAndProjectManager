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
}
