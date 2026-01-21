# Deployment Log Feature - Implementation Complete

## Overview
Added a "Deployment Log" button to the DeploymentDialog that allows users to view deployment pods and their logs in a modern, user-friendly interface.

## New Features

### 1. Deployment Log Button
- **Location**: DeploymentDialog button panel
- **Color**: Blue (#4285F4) - matches modern UI theme
- **Function**: Opens the Deployment Pod List Dialog

### 2. Deployment Pod List Dialog
**Features:**
- Displays all pods for the selected workspace and environment
- Modern table layout with clean design
- Columns displayed:
  - Name (Pod name)
  - Namespace
  - Creation Time
  - App (from annotations.app)
  - Status (from annotations.real_status)
- Double-click on any row to view pod logs
- Refresh button to reload pod list
- Responsive and modern UI design

**API Used:**
- Endpoint: `GET /api/mo-fo/1.0/ops/pod?app_name=`
- Headers:
  - `x-mo-target-env`: environment name
  - `x-mo-target-tenant`: workspace (sub-tenant code)
  - `authorization`: Bearer {workspace_token}

### 3. Deployment Pod Log Dialog
**Features:**
- Black background with white text (console style)
- Displays pod logs in real-time
- Shows pod information at the top:
  - Pod name
  - App name
  - Status
- Refresh button to reload logs
- Proper line break handling (\n converted to actual newlines)
- Modern, clean UI design

**API Used:**
- Endpoint: `GET /api/mo-fo/1.0/ops/pod_logs?pod_name={name}&app_name={app}&previous=false`
- Headers:
  - `x-mo-target-env`: environment name
  - `x-mo-target-tenant`: workspace (sub-tenant code)
  - `authorization`: Bearer {workspace_token}

## Files Created

### 1. DeploymentPod.java
Data model for deployment pods with fields:
- name (metadata.name)
- namespace (metadata.namespace)
- creationTimestamp (metadata.creationTimestamp)
- app (annotations.app)
- realStatus (annotations.real_status)

### 2. DeploymentPodListDialog.java
Dialog for displaying pod list:
- Table with 5 columns
- Double-click to view logs
- Refresh functionality
- Modern UI with Segoe UI font
- Async loading with SwingWorker

### 3. DeploymentPodLogDialog.java
Dialog for displaying pod logs:
- Console-style log viewer (black background, white text)
- Pod information panel at top
- Refresh functionality
- Proper newline handling
- Modern UI design

## Files Modified

### 1. PortalApiClient.java
Added three new methods:
- `getDeploymentPods()` - Query pod list
- `getDeploymentPodLogs()` - Query pod logs
- `parseDeploymentPods()` - Parse pod list response
- `parsePodLogs()` - Parse log response

### 2. DeploymentDialog.java
- Added "Deployment Log" button to button panel
- Added `handleDeploymentLog()` method
- Button validates workspace, environment, and token before opening pod list

## UI Design

### Modern & Youthful Design Elements:
1. **Color Scheme:**
   - Blue buttons (#4285F4) - Google Material Design
   - Clean white backgrounds
   - Subtle borders (#DADCE0)
   - Modern gray text (#3C4043, #5F6368)

2. **Typography:**
   - Segoe UI font family (modern, clean)
   - Bold headers (16px, 14px)
   - Regular text (12px, 13px)
   - Consolas for console logs (monospace)

3. **Layout:**
   - Generous padding and spacing
   - Clean borders and separators
   - Responsive table layouts
   - Modern button styling with hover effects

4. **Console Log Style:**
   - Black background (#000000)
   - White text (#FFFFFF)
   - Monospace font (Consolas 12px)
   - Proper line wrapping
   - Scrollable with always-visible scrollbar

## User Workflow

1. **Open Deployment Dialog:**
   - User selects workspace and environment
   - Workspace token is automatically retrieved

2. **Click "Deployment Log" Button:**
   - Validates workspace, environment, and token
   - Opens Deployment Pod List Dialog

3. **View Pod List:**
   - Table shows all pods for the workspace/environment
   - User can see pod names, namespaces, creation times, apps, and statuses
   - Click "Refresh" to reload the list

4. **View Pod Logs:**
   - Double-click any pod row
   - Opens Deployment Pod Log Dialog
   - Shows pod information and console logs
   - Click "Refresh" to reload logs

## Technical Details

### API Integration:
- Uses existing PortalApiClient infrastructure
- Follows same authentication pattern as other APIs
- Proper error handling and logging
- Async operations with SwingWorker

### Data Flow:
```
User clicks "Deployment Log"
    ↓
Validate workspace, environment, token
    ↓
Open DeploymentPodListDialog
    ↓
Call getDeploymentPods() API
    ↓
Display pods in table
    ↓
User double-clicks pod row
    ↓
Open DeploymentPodLogDialog
    ↓
Call getDeploymentPodLogs() API
    ↓
Display logs in console-style viewer
```

### Error Handling:
- Validates all required fields before API calls
- Shows user-friendly error messages
- Logs all errors for debugging
- Graceful fallback for missing data

## Testing Instructions

1. **Start Application:**
   ```cmd
   java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. **Open Deployment Dialog:**
   - From TenantCICDDialog, click "Deployment" button
   - Or open directly from menu

3. **Test Deployment Log:**
   - Select a workspace (e.g., "stbd")
   - Wait for environment list to load
   - Select an environment
   - Click "Deployment Log" button

4. **Test Pod List:**
   - Verify pod list loads
   - Check all columns display correctly
   - Test "Refresh" button
   - Double-click a pod row

5. **Test Pod Logs:**
   - Verify logs display in console style
   - Check pod information at top
   - Test "Refresh" button
   - Verify line breaks display correctly

## Status
✅ **Implementation Complete**
✅ **Compiled Successfully**
✅ **Packaged Successfully**
✅ **Ready for Testing**

## Next Steps
User should test the deployment log feature:
1. Open Deployment Dialog
2. Select workspace and environment
3. Click "Deployment Log" button
4. View pod list
5. Double-click pod to view logs
6. Verify logs display correctly with proper formatting
