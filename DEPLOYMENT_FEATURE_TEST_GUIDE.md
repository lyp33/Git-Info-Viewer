# Deployment Feature - Quick Test Guide

## Prerequisites

1. **Portal Settings Configuration**
   - Open Portal Settings from CI/CD menu
   - Configure username and password
   - Configure tenant codes with sub-tenant codes format:
     - Example: `stbd{stbddev/stbdtst/stbduat},thailife{thailifedev/thailifetest/thailifeuat}`
   - Save settings

## Test Workflow

### Step 1: Connect to Tenant
1. Open Tenant CI/CD dialog from CI/CD menu
2. Select a tenant from dropdown (e.g., "stbd")
3. Click "Connect" button
4. Wait for successful connection
5. Verify status shows "Connected successfully..."

### Step 2: Search for Build Results
1. Enter search criteria (plan name or app name)
2. Click "Search" button
3. Wait for results to load
4. Verify build results are displayed in table

### Step 3: Select Images for Deployment
1. Select one or more rows from the build results table
2. Note: You can also deploy without selecting rows (manual entry)

### Step 4: Open Deployment Dialog
1. Click "Deployment" button (deep green button)
2. Deployment dialog should open
3. Verify:
   - Image list is pre-filled with selected images (if any were selected)
   - Workspace dropdown shows sub-tenant codes for the connected tenant
   - Environment dropdown is disabled (will enable after workspace selection)
   - Console log area is empty

### Step 5: Select Workspace
1. Select a workspace from the dropdown (e.g., "stbddev")
2. Wait for workspace token retrieval
3. Verify console log shows:
   - "Retrieving token for workspace: [workspace]"
   - "✓ Workspace token retrieved successfully"
   - "Loading environments for workspace: [workspace]"
   - "✓ Loaded X environments"
4. Verify environment dropdown is now enabled and populated

### Step 6: Select Environment
1. Select an environment from the dropdown (e.g., "imo_kic_gemini_sp3")
2. Environment is now ready for deployment

### Step 7: Review/Edit Image List
1. Review the image list in the text area
2. You can:
   - Add more images (one per line)
   - Remove images
   - Edit image names
3. Example image format:
   ```
   docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
   docker-all.repo.ebaotech.com/thailifedev/thailife-web:24.08.22
   ```

### Step 8: Execute Deployment
1. Click "Deploy" button (green button)
2. Review the confirmation dialog showing:
   - Workspace
   - Environment
   - Image count
   - List of images with extracted app names
   - Warning about sequential deployment
3. Click "OK" to proceed or "Cancel" to abort

### Step 9: Monitor Deployment Progress
1. Watch the console log for real-time updates:
   - "Starting Deployment"
   - "→ [1/2] Deploying: [image]"
   - "   App: [app-name]"
   - "   Workspace: [workspace]"
   - "   Environment: [environment]"
   - "✓ [1/2] Deployment successful: [app-name]"
   - "   Result: request success"
2. Deployment button shows "Deploying..." during execution
3. Process stops on first failure

### Step 10: Review Results
1. After completion, console log shows:
   - "Deployment Complete"
   - "Success: X, Failed: Y, Total: Z"
2. Success dialog appears if all deployments succeeded
3. Error dialog appears if any deployment failed
4. Deploy button is re-enabled

## Test Scenarios

### Scenario 1: Successful Single Image Deployment
- Select 1 image from build history
- Deploy to a workspace and environment
- Verify successful deployment
- Check console log for complete flow

### Scenario 2: Successful Multiple Image Deployment
- Select 3-5 images from build history
- Deploy to a workspace and environment
- Verify all deployments succeed
- Check console log shows progress for each image

### Scenario 3: Manual Image Entry
- Don't select any rows from build history
- Open Deployment dialog
- Manually enter image names in text area
- Deploy and verify

### Scenario 4: Deployment Failure Handling
- Enter an invalid image name
- Deploy and verify:
  - Deployment stops on failure
  - Error message is displayed
  - Console log shows error details
  - Subsequent images are not deployed

### Scenario 5: Validation Errors
Test each validation:
- Empty image list → Error: "Please enter at least one image name"
- No workspace selected → Error: "Please select a workspace"
- No environment selected → Error: "Please select an environment"

### Scenario 6: Workspace Without Sub-Tenant Codes
- Configure Portal Settings with simple tenant codes (no sub-tenants)
- Connect to tenant
- Click Deployment button
- Verify warning: "No workspaces configured for tenant"

### Scenario 7: Environment Loading Failure
- Select a workspace with invalid configuration
- Verify error handling and user feedback

## Expected Behavior

### Console Log Format
```
[14:30:15] ✓ Loaded 3 workspaces
[14:30:20] Retrieving token for workspace: thailifedev
[14:30:22] ✓ Workspace token retrieved successfully
[14:30:22] Loading environments for workspace: thailifedev
[14:30:24] ✓ Loaded 5 environments
[14:30:30] ========================================
[14:30:30] Starting Deployment
[14:30:30] Workspace: thailifedev
[14:30:30] Environment: imo_kic_gemini_sp3
[14:30:30] Total Images: 2
[14:30:30] ========================================
[14:30:30] → [1/2] Deploying: docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
[14:30:30]    App: thailife-bs
[14:30:30]    Workspace: thailifedev
[14:30:30]    Environment: imo_kic_gemini_sp3
[14:30:35] ✓ [1/2] Deployment successful: thailife-bs
[14:30:35]    Result: request success
[14:30:35] ----------------------------------------
[14:30:35] → [2/2] Deploying: docker-all.repo.ebaotech.com/thailifedev/thailife-web:24.08.22
[14:30:35]    App: thailife-web
[14:30:35]    Workspace: thailifedev
[14:30:35]    Environment: imo_kic_gemini_sp3
[14:30:40] ✓ [2/2] Deployment successful: thailife-web
[14:30:40]    Result: request success
[14:30:40] ----------------------------------------
[14:30:40] ========================================
[14:30:40] Deployment Complete
[14:30:40] Success: 2, Failed: 0, Total: 2
[14:30:40] ========================================
```

## Troubleshooting

### Issue: Workspace dropdown is empty
- **Cause**: No sub-tenant codes configured for the connected tenant
- **Solution**: Update Portal Settings with sub-tenant codes format

### Issue: Environment dropdown stays disabled
- **Cause**: Workspace token retrieval failed
- **Solution**: Check console log for error details, verify credentials

### Issue: Deployment fails with authentication error
- **Cause**: Workspace token is invalid or expired
- **Solution**: Reselect the workspace to get a new token

### Issue: Image parsing fails
- **Cause**: Invalid image name format
- **Solution**: Use format: `registry/workspace/app:version`

### Issue: Deployment API returns error
- **Cause**: Various reasons (invalid app name, environment not available, etc.)
- **Solution**: Check console log for API error message

## Notes

1. **Token Management**: 
   - Main tenant token is used for connecting to tenant
   - Workspace token is separate and used for deployment
   - Workspace token is retrieved when workspace is selected

2. **Sequential Deployment**:
   - Images are deployed one at a time
   - Process stops on first failure
   - This prevents cascading failures

3. **Image Format**:
   - Full format: `docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22`
   - App name is extracted from the last path segment before the colon
   - Version tag is optional

4. **Console Logging**:
   - All operations are logged with timestamps
   - Visual indicators (✓, ✗, →, ⚠) for easy scanning
   - Auto-scrolls to show latest entries

## Success Criteria

- ✅ Workspace list loads correctly from Portal Settings
- ✅ Workspace token is retrieved successfully
- ✅ Environment list loads from tenant configuration
- ✅ Image names are parsed correctly
- ✅ Validation catches all invalid configurations
- ✅ Confirmation dialog shows accurate deployment plan
- ✅ Deployment executes sequentially
- ✅ Console log shows real-time progress
- ✅ Deployment stops on first failure
- ✅ Success/failure counts are accurate
- ✅ Error messages are clear and actionable
- ✅ UI remains responsive during deployment
- ✅ Resources are cleaned up on dialog close
