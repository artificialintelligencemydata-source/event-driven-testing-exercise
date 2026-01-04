package com.acuver.autwit.internal.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JiraServiceProvider {
    private final String jiraUrl;
    private final String projectKey;
    private final String authHeader;
    private final HttpClient httpClient;
    private static final Logger log = LogManager.getLogger(JiraServiceProvider.class);
    public JiraServiceProvider(String jiraUrl, String username, String apiToken, String projectKey) {
        this.jiraUrl = jiraUrl.endsWith("/") ? jiraUrl.substring(0, jiraUrl.length() - 1) : jiraUrl;
        this.projectKey = projectKey;
        this.httpClient = HttpClient.newHttpClient();

        String auth = username + ":" + apiToken;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        log.debug(" Jira URL: {}", this.jiraUrl);
        log.debug(" Jira User: {}", username);
        log.debug("Project Key: {}", projectKey);
    }

    /**
     * Creates a Jira issue in Cloud (v3 API). Checks for duplicates by summary before creating.
     */
    public void createJiraTicket(String issueType, String summary, String description,
                                 String reporterName, String assignee) {

        try {
            // 1️⃣ Search for duplicates
            String jql = String.format(
                    "project = %s AND issuetype in (Bug, Task, Story) AND status != Closed " +
                            "AND resolution = Unresolved AND summary ~ \"%s\"",
                    projectKey, summary.replace("\"", "\\\"")
            );

            String searchUrl = jiraUrl + "/rest/api/3/search/jql";
            JSONObject searchBody = new JSONObject()
                    .put("jql", jql)
                    .put("maxResults", 5);

            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", authHeader)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(searchBody.toString()))
                    .build();

            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());

            if (searchResponse.statusCode() != 200) {
                log.error(" Jira search failed with status {}: {}", searchResponse.statusCode(), searchResponse.body());
                return;
            }

            /*JSONArray issues = new JSONObject(searchResponse.body()).optJSONArray("issues");
            if (issues.length() > 0) {
                log.warn("⚠ Duplicate issue(s) found for summary '{}':", summary);
                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.getJSONObject(i);
                    log.warn("   {} → {}", issue.getString("key"),
                            issue.getJSONObject("fields").optString("summary"));
                }
                return; // Skip duplicate creation
            }*/
            JSONObject searchJson;
            try {
                searchJson = new JSONObject(searchResponse.body());
            } catch (JSONException e) {
                log.error("❌ Invalid JSON in Jira search response: {}", searchResponse.body());
                return;
            }

            JSONArray issues = searchJson.optJSONArray("issues");
            if (issues == null || issues.length() == 0) {
                log.debug("✅ No duplicate issues found for summary '{}'", summary);
            } else {
                log.warn("⚠ Duplicate issue(s) found for summary '{}':", summary);
                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issue = issues.optJSONObject(i);
                    if (issue == null) continue;

                    String key = issue.optString("key", "UNKNOWN");

                    JSONObject fields = issue.optJSONObject("fields");
                    String issueSummary = (fields != null)
                            ? fields.optString("summary", "(no summary)")
                            : "(no fields)";

                    log.warn("   {} → {}", key, issueSummary);
                }
                return; // Skip duplicate creation
            }



            // 2️⃣ Build ADF description for Jira Cloud
            JSONObject adfDescription = new JSONObject()
                    .put("type", "doc")
                    .put("version", 1)
                    .put("content", new JSONArray()
                            .put(new JSONObject()
                                    .put("type", "paragraph")
                                    .put("content", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("type", "text")
                                                    .put("text", description)
                                            )
                                    )
                            )
                    );

            // 3️⃣ Create new Jira issue
            String createUrl = jiraUrl + "/rest/api/3/issue";
            JSONObject issueBody = new JSONObject()
                    .put("fields", new JSONObject()
                            .put("project", new JSONObject().put("key", projectKey))
                            .put("summary", summary)
                            .put("description", adfDescription)
                            .put("issuetype", new JSONObject().put("name", issueType))
                    );

            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(createUrl))
                    .header("Authorization", authHeader)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(issueBody.toString()))
                    .build();

            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

            if (createResponse.statusCode() == 201) {
                JSONObject issue = new JSONObject(createResponse.body());
                log.debug(" Jira issue created successfully → {} ({}browse/{})",
                        issue.getString("key"), jiraUrl + "/", issue.getString("key"));
            } else {
                log.error(" Jira issue creation failed with status {}: {}",
                        createResponse.statusCode(), createResponse.body());
            }

        } catch (IOException | InterruptedException e) {
            log.error("⚠ Exception while creating Jira issue", e);
            Thread.currentThread().interrupt();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
