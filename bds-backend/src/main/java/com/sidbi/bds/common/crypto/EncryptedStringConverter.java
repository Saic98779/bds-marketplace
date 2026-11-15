package com.sidbi.bds.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return FieldCrypto.get().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return FieldCrypto.get().decrypt(dbData);
    }
}
