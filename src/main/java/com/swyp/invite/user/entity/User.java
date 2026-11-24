package com.swyp.invite.user.entity;

import com.swyp.invite.global.PiiEncryptionConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kakaoId;

    private String nickname;

    @Convert(converter = PiiEncryptionConverter.class)
    private String email;

    @Convert(converter = PiiEncryptionConverter.class)
    private String settlementInfo;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public User(String kakaoId, String nickname) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
    }

    public void updateProfile(String nickname, String email, String settlementInfo) {
        this.nickname = nickname;
        this.email = email;
        this.settlementInfo = settlementInfo;
    }
}
