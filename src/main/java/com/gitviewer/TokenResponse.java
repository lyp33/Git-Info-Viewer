package com.gitviewer;

/**
 * Token API响应数据模型
 * Represents token API response from Portal
 */
public class TokenResponse {
    private String accessToken;
    private long expireIn;
    private String message;
    private String errCode;
    private boolean authResult;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public TokenResponse() {
        this.accessToken = "";
        this.expireIn = 0;
        this.message = "";
        this.errCode = "";
        this.authResult = false;
    }
    
    // Getters and setters
    public String getAccessToken() {
        return accessToken != null ? accessToken : "";
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public long getExpireIn() {
        return expireIn;
    }
    
    public void setExpireIn(long expireIn) {
        this.expireIn = expireIn;
    }
    
    public String getMessage() {
        return message != null ? message : "";
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrCode() {
        return errCode != null ? errCode : "";
    }
    
    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }
    
    public boolean isAuthResult() {
        return authResult;
    }
    
    public void setAuthResult(boolean authResult) {
        this.authResult = authResult;
    }
    
    /**
     * 检查认证是否成功
     * @return true if authentication was successful
     */
    public boolean isSuccess() {
        return authResult && (errCode == null || errCode.isEmpty());
    }
    
    @Override
    public String toString() {
        return "TokenResponse{" +
                "accessToken='" + (accessToken != null && accessToken.length() > 8 ? 
                    accessToken.substring(0, 4) + "..." + accessToken.substring(accessToken.length() - 4) : "[MASKED]") + '\'' +
                ", expireIn=" + expireIn +
                ", message='" + message + '\'' +
                ", errCode='" + errCode + '\'' +
                ", authResult=" + authResult +
                '}';
    }
}
