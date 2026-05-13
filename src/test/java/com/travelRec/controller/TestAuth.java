package com.travelRec.controller;

import com.travelRec.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

final class TestAuth {

    private TestAuth() {}

    static RequestPostProcessor user(Long id, String email, String role) {
        return request -> {
            CustomUserDetails principal = new CustomUserDetails(
                    id, email,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    static RequestPostProcessor user() {
        return user(1L, "anna@mail.com", "USER");
    }
}