package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 密码加密工具类
 * Password encryption utility using AES-256
 */
public class PasswordEncryption {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncryption.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 32; // 256 bits
    private static final int IV_SIZE = 16;  // 128 bits
    
    // 从机器ID生成密钥
    private static final byte[] KEY = generateKey();
    private static final byte[] IV = generateIV();
    
    /**
     * 加密密码
     * Encrypt password using AES-256
     * 
     * @param password 明文密码
     * @return Base64编码的加密密码，如果加密失败则返回null
     */
    public static String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            logger.debug("encrypt: Password is null or empty");
            return "";
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(encrypted);
            
            logger.debug("encrypt: Password encrypted successfully");
            return result;
        } catch (Exception e) {
            logger.error("encrypt: Failed to encrypt password", e);
            return null;
        }
    }
    
    /**
     * 解密密码
     * Decrypt password using AES-256
     * 
     * @param encryptedPassword Base64编码的加密密码
     * @return 明文密码，如果解密失败则返回null
     */
    public static String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            logger.debug("decrypt: Encrypted password is null or empty");
            return "";
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(KEY, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            String result = new String(decrypted, StandardCharsets.UTF_8);
            
            logger.debug("decrypt: Password decrypted successfully");
            return result;
        } catch (Exception e) {
            logger.error("decrypt: Failed to decrypt password", e);
            return null;
        }
    }
    
    /**
     * 从机器ID生成加密密钥
     * Generate encryption key from machine ID
     * 
     * @return 32字节的密钥
     */
    private static byte[] generateKey() {
        try {
            // 使用系统属性生成唯一的机器标识
            String machineId = System.getProperty("user.name") + 
                             System.getProperty("os.name") + 
                             System.getProperty("user.home");
            
            // 使用SHA-256生成固定长度的密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(machineId.getBytes(StandardCharsets.UTF_8));
            
            // 确保密钥长度为32字节
            return Arrays.copyOf(hash, KEY_SIZE);
        } catch (Exception e) {
            logger.error("generateKey: Failed to generate key, using default", e);
            // 如果生成失败，使用默认密钥（不推荐，但保证程序可运行）
            return Arrays.copyOf("GitViewerDefaultKey123456789012".getBytes(StandardCharsets.UTF_8), KEY_SIZE);
        }
    }
    
    /**
     * 生成初始化向量IV
     * Generate initialization vector
     * 
     * @return 16字节的IV
     */
    private static byte[] generateIV() {
        try {
            // 使用固定的IV（简化实现，实际生产环境应使用随机IV并与密文一起存储）
            String ivSource = "GitViewerIV12345";
            return Arrays.copyOf(ivSource.getBytes(StandardCharsets.UTF_8), IV_SIZE);
        } catch (Exception e) {
            logger.error("generateIV: Failed to generate IV, using default", e);
            return new byte[IV_SIZE];
        }
    }
    
    /**
     * 测试加密解密功能
     * Test encryption and decryption
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 测试加密解密
        String originalPassword = "TestPassword123!@#";
        System.out.println("Original: " + originalPassword);
        
        String encrypted = encrypt(originalPassword);
        System.out.println("Encrypted: " + encrypted);
        
        String decrypted = decrypt(encrypted);
        System.out.println("Decrypted: " + decrypted);
        
        System.out.println("Match: " + originalPassword.equals(decrypted));
    }
}
