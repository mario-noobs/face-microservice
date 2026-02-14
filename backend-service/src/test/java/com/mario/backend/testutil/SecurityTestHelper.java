package com.mario.backend.testutil;

import com.mario.backend.auth.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static com.mario.backend.testutil.TestConstants.*;

public final class SecurityTestHelper {

    private SecurityTestHelper() {}

    public static void setAuthentication(Long userId, String email, String role, List<String> permissions) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, email, role, permissions);

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void setBasicUserAuthentication() {
        setAuthentication(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
    }

    public static void setPremiumUserAuthentication() {
        setAuthentication(USER_ID, USER_EMAIL, ROLE_PREMIUM_USER, PREMIUM_USER_PERMISSIONS);
    }

    public static void setSuperAdminAuthentication() {
        setAuthentication(ADMIN_USER_ID, ADMIN_EMAIL, ROLE_SUPERADMIN, SUPERADMIN_PERMISSIONS);
    }

    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
