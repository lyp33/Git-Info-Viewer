# Deployment Feature - Quick Start Guide

## 🚀 Quick Start (5 Steps)

### 1️⃣ Configure Portal Settings
```
CI/CD Menu → Portal Settings
- Username: [your portal username]
- Password: [your portal password]
- Tenant Codes: stbd{stbddev/stbdtst},thailife{thailifedev/thailifetest}
```

### 2️⃣ Connect to Tenant
```
CI/CD Menu → Tenant CI/CD
- Select Tenant: stbd
- Click: Connect
- Wait for: "Connected successfully..."
```

### 3️⃣ Search Build Results
```
- Enter Plan Name or App Name
- Click: Search
- Select images from results table
```

### 4️⃣ Open Deployment Dialog
```
- Click: Deployment (green button)
- Select Workspace: stbddev
- Wait for environments to load
- Select Environment: imo_kic_gemini_sp3
```

### 5️⃣ Deploy
```
- Review image list
- Click: Deploy
- Confirm deployment
- Monitor console log
```

## 📋 Sub-Tenant Code Format

### Simple Format
```
tenant1,tenant2,tenant3
```

### With Workspaces
```
tenant{workspace1/workspace2/workspace3}
```

### Mixed Format
```
stbd{stbddev/stbdtst/stbduat},thailife{thailifedev/thailifetest/thailifeuat}
```

## 🎯 Key Features

- ✅ **Workspace Selection**: Choose from configured sub-tenant codes
- ✅ **Environment Discovery**: Automatically loads available environments
- ✅ **Image Selection**: Select from build history or enter manually
- ✅ **Sequential Deployment**: One image at a time, stops on failure
- ✅ **Real-time Logging**: Console shows progress with timestamps
- ✅ **Error Handling**: Clear error messages at every step

## 🔍 Console Log Indicators

- `✓` Success
- `✗` Failure
- `→` In Progress
- `⚠` Warning

## 📝 Image Format Examples

```
docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
docker-all.repo.ebaotech.com/thailifedev/thailife-web:24.08.22
registry/workspace/app:version
workspace/app:version
app:version
```

## ⚠️ Important Notes

1. **Token Management**: Workspace token is separate from main tenant token
2. **Sequential Deployment**: Images deploy one at a time
3. **Stop on Failure**: Process halts if any image fails
4. **App Name Extraction**: Automatically extracted from image name

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Workspace dropdown empty | Configure sub-tenant codes in Portal Settings |
| Environment dropdown disabled | Workspace token retrieval failed, check credentials |
| Deployment fails | Check console log for API error details |
| Image parsing fails | Use format: `registry/workspace/app:version` |

## 📞 Need Help?

1. Check console log for detailed error messages
2. Review `DEPLOYMENT_FEATURE_TEST_GUIDE.md` for detailed testing steps
3. Review `DEPLOYMENT_FEATURE_IMPLEMENTATION.md` for technical details

## 🎉 Success!

When deployment completes successfully, you'll see:
```
[14:30:40] ========================================
[14:30:40] Deployment Complete
[14:30:40] Success: 2, Failed: 0, Total: 2
[14:30:40] ========================================
```

---

**Ready to Deploy?** Follow the 5 steps above! 🚀
