package com.cars24.rcview.service;

import com.cars24.rcview.dto.UserInfoDto;
import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.repository.AppUserRepository;
import com.cars24.rcview.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    public UserInfoDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return UserInfoDto.from(user.getAppUser());
        }
        return null;
    }
}
