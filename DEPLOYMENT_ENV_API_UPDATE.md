# Deployment Environment API Update

## Summary

Updated the DeploymentDialog to use a new API endpoint for retrieving environment information instead of the previous "get tenant config info" API.

## Changes Made

### 1. PortalApiClient.java

Added new method `getEnvironments()`:

```java
/**
 * 获取环境列表
 * Get environment list using new API
 * 
 * @param workspaceToken 工作空间Token
 * @return 环境名称列表
 * @throws IOException 网络错误或API错误
 */
public List<String> getEnvironments(String workspaceToken) throws IOException
```

**API Details:**
- **Endpoint**: `GET /api/mo-fo/1.0/ops/env?status=&all=false`
- **Authentication**: Bearer token (workspace token)
- **Response**: JSON array of environment objects
- **Data Extraction**: Extracts `env_name` field from each object

### 2. DeploymentDialog.java

Updated `loadEnvironmentList()` method:

**Before:**
- Called `apiClient.getTenantConfiguration()` 
- Parsed `deploy_pipeline.pipeline` array
- Extracted `env_name` from pipeline entries

**After:**
- Calls `apiClient.getEnvironments(workspaceToken)`
- Directly receives list of environment names
- Simpler and more direct approach

## Benefits

1. **Simplified Logic**: No need to parse complex tenant configuration structure
2. **Dedicated API**: Uses a specific API designed for environment retrieval
3. **Better Performance**: Lighter API call with only necessary data
4. **Cleaner Code**: Reduced complexity in environment loading logic

## Testing

To test the changes:

1. Open the application
2. Navigate to Tenant CI/CD dialog
3. Click "Deployment" button
4. Select a workspace from the dropdown
5. Verify that environments are loaded correctly in the Environment dropdown

The new API should return the same environment names as before, but through a more direct endpoint.

## API Comparison

### Old API
```
GET /api/mo-fo/1.0/ops/tenant/config
Response: {
  "deploy_pipeline": {
    "pipeline": [
      { "env_name": "portal" },
      { "env_name": "aws_sg_insuremo_portal" }
    ]
  }
}
```

### New API
```
GET /api/mo-fo/1.0/ops/env?status=&all=false
Response: [
  { "env_name": "aws_sg_insuremo_portal", ... },
  { "env_name": "portal", ... }
]
```

## Files Modified

- `src/main/java/com/gitviewer/PortalApiClient.java` - Added `getEnvironments()` method
- `src/main/java/com/gitviewer/DeploymentDialog.java` - Updated `loadEnvironmentList()` method

## Date

2026-01-21
