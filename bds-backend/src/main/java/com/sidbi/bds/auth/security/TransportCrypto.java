package com.sidbi.bds.auth.security;

import com.sidbi.bds.auth.config.AuthProperties;
import com.sidbi.bds.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class TransportCrypto {

    private static final String PREFIX = "rsa:";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final boolean requireEncrypted;
    private final String keyId;

    public TransportCrypto(AuthProperties properties) throws GeneralSecurityException {
        this.requireEncrypted = properties.isRequireEncryptedPassword();
        if (properties.getTransportPrivateKey() != null && !properties.getTransportPrivateKey().isBlank()
                && properties.getTransportPublicKey() != null && !properties.getTransportPublicKey().isBlank()) {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            this.privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(properties.getTransportPrivateKey())));
            this.publicKey = factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(properties.getTransportPublicKey())));
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
        }
        this.keyId = Integer.toHexString(java.util.Arrays.hashCode(publicKey.getEncoded()));
    }

    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String keyId() {
        return keyId;
    }

    public String reveal(String value) {
        if (value == null) {
            return null;
        }
        if (!value.startsWith(PREFIX)) {
            if (requireEncrypted) {
                throw BusinessException.badRequest("Sensitive fields must be encrypted before transmission");
            }
            return value;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey, new OAEPParameterSpec("SHA-256", "MGF1",
                    MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(value.substring(PREFIX.length())));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw BusinessException.badRequest("Encrypted value could not be read. Refresh the page and try again.");
        }
    }
}
