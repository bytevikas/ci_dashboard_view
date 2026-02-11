package com.cars24.rcview.security;

import com.cars24.rcview.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final AppUser appUser;

    public CustomOAuth2User(OAuth2User delegate, AppUser appUser) {
        this.delegate = delegate;
        this.appUser = appUser;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (delegate == null) {
            return Map.of("email", appUser.getEmail(), "name", appUser.getName() != null ? appUser.getName() : "");
        }
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (delegate == null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
        }
        return Stream.concat(
                delegate.getAuthorities().stream(),
                Stream.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))
        ).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        if (delegate == null) {
            return appUser.getEmail();
        }
        return delegate.getAttribute("email");
    }
}
