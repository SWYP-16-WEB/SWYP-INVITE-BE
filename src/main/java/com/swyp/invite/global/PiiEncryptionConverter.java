package com.swyp.invite.global;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter
public class PiiEncryptionConverter implements AttributeConverter<String, String> {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int KEY_BYTES = 32;
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;

	private final SecretKey key;
	private final SecureRandom random = new SecureRandom();

	public PiiEncryptionConverter(@Value("${PII_ENCRYPTION_KEY}") String base64Key) {
		this.key = new SecretKeySpec(decodeKey(base64Key), "AES");
	}

	private static byte[] decodeKey(String base64Key) {
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(base64Key);
		}
		catch (IllegalArgumentException e) {
			keyBytes = new byte[0];
		}
		if (keyBytes.length != KEY_BYTES) {
			throw new IllegalArgumentException("PII_ENCRYPTION_KEY는 Base64로 적은 32바이트 값이어야 합니다");
		}
		return keyBytes;
	}

	@Override
	public String convertToDatabaseColumn(String plain) {
		if (plain == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_BYTES];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] stored = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
			return Base64.getEncoder().encodeToString(stored);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("개인정보 암호화에 실패했습니다", e);
		}
	}

	@Override
	public String convertToEntityAttribute(String stored) {
		if (stored == null) {
			return null;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(stored);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
			byte[] plain = cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES);
			return new String(plain, StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IllegalStateException("개인정보 복호화에 실패했습니다", e);
		}
	}

}
