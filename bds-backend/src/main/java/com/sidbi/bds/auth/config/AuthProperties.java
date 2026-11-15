package com.sidbi.bds.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bds.auth")
public class AuthProperties {

    private boolean loginOtpEnabled = true;
    private boolean registrationOtpRequired = true;
    private boolean exposeOtpInResponse = false;
    private int otpExpiryMinutes = 5;
    private int otpMaxAttempts = 5;
    private int otpResendSeconds = 30;
    private int maxLoginAttempts = 5;
    private int lockMinutes = 15;
    private boolean buyerAutoApprove = false;
    private int reverificationMonths = 12;
    private boolean requireEncryptedPassword = false;
    private String transportPrivateKey;
    private String transportPublicKey;
    private Bootstrap bootstrap = new Bootstrap();

    public boolean isLoginOtpEnabled() {
        return loginOtpEnabled;
    }

    public void setLoginOtpEnabled(boolean loginOtpEnabled) {
        this.loginOtpEnabled = loginOtpEnabled;
    }

    public boolean isRegistrationOtpRequired() {
        return registrationOtpRequired;
    }

    public void setRegistrationOtpRequired(boolean registrationOtpRequired) {
        this.registrationOtpRequired = registrationOtpRequired;
    }

    public boolean isExposeOtpInResponse() {
        return exposeOtpInResponse;
    }

    public void setExposeOtpInResponse(boolean exposeOtpInResponse) {
        this.exposeOtpInResponse = exposeOtpInResponse;
    }

    public int getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }

    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public int getOtpMaxAttempts() {
        return otpMaxAttempts;
    }

    public void setOtpMaxAttempts(int otpMaxAttempts) {
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public int getOtpResendSeconds() {
        return otpResendSeconds;
    }

    public void setOtpResendSeconds(int otpResendSeconds) {
        this.otpResendSeconds = otpResendSeconds;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public void setLockMinutes(int lockMinutes) {
        this.lockMinutes = lockMinutes;
    }

    public boolean isBuyerAutoApprove() {
        return buyerAutoApprove;
    }

    public void setBuyerAutoApprove(boolean buyerAutoApprove) {
        this.buyerAutoApprove = buyerAutoApprove;
    }

    public int getReverificationMonths() {
        return reverificationMonths;
    }

    public void setReverificationMonths(int reverificationMonths) {
        this.reverificationMonths = reverificationMonths;
    }

    public boolean isRequireEncryptedPassword() {
        return requireEncryptedPassword;
    }

    public void setRequireEncryptedPassword(boolean requireEncryptedPassword) {
        this.requireEncryptedPassword = requireEncryptedPassword;
    }

    public String getTransportPrivateKey() {
        return transportPrivateKey;
    }

    public void setTransportPrivateKey(String transportPrivateKey) {
        this.transportPrivateKey = transportPrivateKey;
    }

    public String getTransportPublicKey() {
        return transportPublicKey;
    }

    public void setTransportPublicKey(String transportPublicKey) {
        this.transportPublicKey = transportPublicKey;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public static class Bootstrap {
        private String adminEmail = "admin@bdsmarketplace.in";
        private String adminPassword;
        private String adminName = "Platform Administrator";
        private boolean demoUsers = false;
        private String demoPassword;

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getAdminName() {
            return adminName;
        }

        public void setAdminName(String adminName) {
            this.adminName = adminName;
        }

        public boolean isDemoUsers() {
            return demoUsers;
        }

        public void setDemoUsers(boolean demoUsers) {
            this.demoUsers = demoUsers;
        }

        public String getDemoPassword() {
            return demoPassword;
        }

        public void setDemoPassword(String demoPassword) {
            this.demoPassword = demoPassword;
        }
    }
}
