package com.acuver.autwit.internal.integration;


import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.listeners.TestNGListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Example TestNG class that creates an issue in Jira using Jira Cloud v3 API.
 * Ensure you have a valid API token, email, and project key before running.
 */
public class CreateIssue {
    private static final Logger log = LogManager.getLogger(CreateIssue.class);
    // Replace these with your actual configuration values or read from a config file
    private static final String jiraUrl = FileReaderManager.getInstance().getConfigReader().getJiraURL();
    private static final String jiraUserEmail = FileReaderManager.getInstance().getConfigReader().getJiraUserName();
    private static final String jiraApiToken = FileReaderManager.getInstance().getConfigReader().getJiraAPIToken();
    private static final String jiraProjectKey = FileReaderManager.getInstance().getConfigReader().getJiraProjectName();           // Atlassian API token

    /**
     * Example TestNG test method that creates a Jira issue.
     */
    @Test
    public void createIssue(String issueType, String summary, String description) throws IOException {
        // Initialize the Jira API service
        JiraServiceProvider jiraService = new JiraServiceProvider(
                jiraUrl,
                jiraUserEmail,
                jiraApiToken,
                jiraProjectKey
        );


        // Call the method to create issue
        jiraService.createJiraTicket(issueType, summary, description, "Yogini Naik", "");
    }
}

