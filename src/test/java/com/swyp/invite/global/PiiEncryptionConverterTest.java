package com.swyp.invite.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class PiiEncryptionConverterTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final PiiEncryptionConverter converter = new PiiEncryptionConverter(KEY);

    @Test
    void 암호화한_값을_되돌리면_원문과_같다() {
        String stored = converter.convertToDatabaseColumn("user1@example.com");

        assertThat(stored).isNotEqualTo("user1@example.com");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo("user1@example.com");
    }

    @Test
    void 같은_원문도_저장할_때마다_암호문이_다르다() {
        String first = converter.convertToDatabaseColumn("user1@example.com");
        String second = converter.convertToDatabaseColumn("user1@example.com");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void null은_null로_둔다() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void 원문_356바이트의_암호문은_512자다() {
        assertThat(converter.convertToDatabaseColumn("a".repeat(356))).hasSize(512);
    }

    @Test
    void 한글_118자의_암호문은_512자를_넘지_않는다() {
        assertThat(converter.convertToDatabaseColumn("가".repeat(118)).length()).isLessThanOrEqualTo(512);
    }

    @Test
    void 암호문이_바뀌면_복호화에_실패한다() {
        byte[] bytes = Base64.getDecoder().decode(converter.convertToDatabaseColumn("user1@example.com"));
        bytes[bytes.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 키가_32바이트가_아니면_만들_수_없다() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new PiiEncryptionConverter(shortKey)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 키가_비어_있으면_만들_수_없다() {
        assertThatThrownBy(() -> new PiiEncryptionConverter("")).isInstanceOf(IllegalArgumentException.class);
    }
}
