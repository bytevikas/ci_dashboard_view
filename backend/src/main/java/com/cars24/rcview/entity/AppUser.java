package com.cars24.rcview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String pictureUrl;

    @Indexed
    private Role role;

    private boolean ssoEnabled;

    private Instant createdAt;
    private Instant updatedAt;

    public enum Role {
        USER,
        ADMIN,
        SUPER_ADMIN
    }
}
