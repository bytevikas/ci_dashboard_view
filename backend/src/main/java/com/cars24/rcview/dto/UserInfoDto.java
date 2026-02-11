package com.cars24.rcview.dto;

import com.cars24.rcview.entity.AppUser;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDto {

    private String id;
    private String email;
    private String name;
    private String pictureUrl;
    private AppUser.Role role;
    private boolean ssoEnabled;

    public static UserInfoDto from(AppUser u) {
        return UserInfoDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .pictureUrl(u.getPictureUrl())
                .role(u.getRole())
                .ssoEnabled(u.isSsoEnabled())
                .build();
    }
}
