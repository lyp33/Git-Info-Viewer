# Jenkins CI/CD Integration Guide

## Overview

The Git Info Viewer application now includes Jenkins CI/CD integration, allowing you to browse Jenkins job hierarchies, view build details, and trigger builds directly from the application.

## Features

### 1. CI/CD Menu
A new "CI/CD" menu has been added to the menu bar with the following options:
- **Core/SDK Build**: Browse Jenkins job hierarchy and manage builds
- **Tenant CI/CD**: Placeholder for future tenant-specific CI/CD functionality
- **Jenkins Settings**: Configure Jenkins server connection

### 2. Jenkins Settings
Configure your Jenkins server connection:
- **Jenkins URL**: The base URL of your Jenkins server (e.g., `http://172.25.32.166:8080`)
- **Username**: Your Jenkins username
- **API Token**: Your Jenkins API token (generated from your Jenkins user profile)
- **Default Job Path**: The default job path to browse (e.g., `job/gemini`)

**To configure:**
1. Go to `CI/CD` → `Jenkins Settings...`
2. Enter your Jenkins server details
3. Click "Test Connection" to verify the connection
4. Click "Save" to save the settings

### 3. Job Browser
Browse the Jenkins job hierarchy:
- Navigate through folders and jobs in a tree structure
- Folders can be expanded to show their contents
- Double-click on a leaf job to open the job details dialog
- **Console Log Panel**: The bottom panel shows debug information including:
  - Extracted job paths from URLs
  - Full API URLs being requested
  - Loading status and error messages
  - Use "Clear Log" button to clear the console

**To use:**
1. Go to `CI/CD` → `Core/SDK Build...`
2. The browser will load the job hierarchy from the configured default path
3. Expand folders to navigate through the hierarchy
4. Double-click on a job to view its details
5. Check the console log panel at the bottom for debugging information

### 4. Job Details
View detailed information about a Jenkins job:
- **Build History**: List of recent builds with status indicators
  - Green (●): Successful build
  - Red (●): Failed build
  - Blue (●): Build in progress
- **Stage View**: Visual representation of pipeline stages
  - Green background: Successful stage
  - Red background: Failed stage
  - Blue background: Stage in progress
  - Gray background: Stage not executed
- **Build with Parameters**: Trigger a new build with custom parameters
- **Rebuild**: Right-click on a build to rebuild with the same parameters

**To use:**
1. Double-click on a job in the Job Browser
2. View the build history and select a build to see its stage view
3. Click "Build with Parameters" to trigger a new build
4. Right-click on a build and select "Rebuild" to rebuild with the same parameters

### 5. Build Parameters
Configure and trigger builds with parameters:
- **String Parameters**: Text input fields
- **Choice Parameters**: Dropdown menus with predefined options
- **Boolean Parameters**: Checkboxes
- **Text Parameters**: Multi-line text areas

**To use:**
1. Click "Build with Parameters" in the Job Details dialog
2. Fill in the required parameters
3. Click "Build" to trigger the build
4. The build will be queued and the build history will refresh automatically

## Configuration Example

### Jenkins Server Configuration
```
Jenkins URL: http://172.25.32.166:8080
Username: your-username
API Token: your-api-token
Default Job Path: job/gemini
```

### Generating Jenkins API Token
1. Log in to your Jenkins server
2. Click on your username in the top-right corner
3. Click "Configure"
4. Scroll down to "API Token" section
5. Click "Add new Token"
6. Give it a name and click "Generate"
7. Copy the generated token and paste it into the Jenkins Settings dialog

## Troubleshooting

### Connection Failed
- Verify the Jenkins URL is correct and accessible
- Check that your username and API token are correct
- Ensure your network allows access to the Jenkins server
- Check if Jenkins requires authentication

### Job Hierarchy Not Loading
- Verify the default job path is correct (e.g., `job/gemini`)
- Check that you have permission to access the job
- Try refreshing the browser
- Check the console log panel at the bottom of the Job Browser for detailed error messages and API URLs

### Build Trigger Failed
- Verify you have permission to trigger builds for the job
- Check that all required parameters are filled in
- Ensure the Jenkins server is not overloaded

### Stage View Not Available
- Some jobs may not have stage information available
- Check that the job is a pipeline job
- Verify the build has completed or is in progress

## Notes

- All Jenkins settings are saved to the application settings file (`gitviewer.properties` in your home directory)
- The application uses the Jenkins REST API for all operations
- Build history is automatically refreshed when the Job Details dialog gains focus
- The application supports both authenticated and unauthenticated Jenkins servers

## Future Enhancements

- Tenant CI/CD functionality (currently a placeholder)
- Build console output viewer
- Build artifact download
- Pipeline visualization
- Build queue management
