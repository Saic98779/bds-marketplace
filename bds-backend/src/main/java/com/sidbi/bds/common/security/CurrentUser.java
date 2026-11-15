package com.sidbi.bds.common.security;

import com.sidbi.bds.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class CurrentUser {

    /** Lets an administrator name the seller or buyer whose account a seller/buyer endpoint should act on. */
    public static final String ACT_AS_HEADER = "X-Act-As-User";

    private CurrentUser() {
    }

    public static Optional<AuthUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static AuthUser get() {
        return find().orElseThrow(() -> BusinessException.unauthorized("Authentication required"));
    }

    public static Long id() {
        return get().userId();
    }

    public static void require(Permission permission) {
        if (!get().has(permission)) {
            throw BusinessException.forbidden("Privilege " + permission.name() + " has not been granted to your account");
        }
    }

    /** The user named in {@link #ACT_AS_HEADER} by an administrator. Always empty for other roles. */
    public static Optional<Long> actAs() {
        if (!get().isAdmin() || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        String value = attributes.getRequest().getHeader(ACT_AS_HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value.trim()));
        } catch (NumberFormatException ex) {
            throw BusinessException.badRequest(ACT_AS_HEADER + " must be a numeric user id");
        }
    }

    /** The user whose own account a "my account" endpoint works on. Administrators must name one with {@link #ACT_AS_HEADER}. */
    public static Long subjectId() {
        AuthUser user = get();
        if (!user.isAdmin()) {
            return user.userId();
        }
        return actAs().orElseThrow(() -> BusinessException.badRequest(
                "Administrators must specify the target user in the " + ACT_AS_HEADER + " header"));
    }

    /** Owner filter for list queries: the caller's id, or null (all records) for an administrator not acting as a specific user. */
    public static Long scopeId() {
        AuthUser user = get();
        return user.isAdmin() ? actAs().orElse(null) : user.userId();
    }

    /** Whether the caller may act on a record owned by {@code ownerId}. Administrators may act on any record. */
    public static boolean owns(Long ownerId) {
        Long scope = scopeId();
        return scope == null || scope.equals(ownerId);
    }
}
