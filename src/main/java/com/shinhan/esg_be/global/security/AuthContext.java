package com.shinhan.esg_be.global.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class AuthContext {

    public CustomUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "인증된 사용자 정보가 없습니다.");
        }
        return principal;
    }

    public Long currentUserId() {
        return currentPrincipal().userId();
    }

    public String currentLoginId() {
        return currentPrincipal().loginId();
    }
}
