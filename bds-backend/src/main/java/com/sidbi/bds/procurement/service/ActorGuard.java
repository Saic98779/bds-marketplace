package com.sidbi.bds.procurement.service;

import com.sidbi.bds.common.client.IdentityClient;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.exception.BusinessException;
import com.sidbi.bds.common.security.AuthUser;
import com.sidbi.bds.common.security.CurrentUser;
import com.sidbi.bds.common.security.Permission;
import com.sidbi.bds.common.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActorGuard {

    private final IdentityClient identityClient;

    public AuthUser seller(Permission permission) {
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            return user;
        }
        if (!user.isSeller()) {
            throw BusinessException.forbidden("This action is available to service providers only");
        }
        verify(user, permission, "service provider");
        return user;
    }

    public AuthUser buyer(Permission permission) {
        AuthUser user = CurrentUser.get();
        if (user.isAdmin()) {
            return user;
        }
        if (!user.isBuyer()) {
            throw BusinessException.forbidden("This action is available to purchasers only");
        }
        verify(user, permission, "purchaser");
        return user;
    }

    /**
     * The seller an action is performed as. Administrators act on behalf of the seller named in
     * {@link CurrentUser#ACT_AS_HEADER}; everyone else acts as themselves.
     */
    public AuthUser sellerActor(Permission permission) {
        return CurrentUser.get().isAdmin() ? onBehalfOf(Role.SELLER) : seller(permission);
    }

    /** The buyer an action is performed as; see {@link #sellerActor(Permission)}. */
    public AuthUser buyerActor(Permission permission) {
        return CurrentUser.get().isAdmin() ? onBehalfOf(Role.BUYER) : buyer(permission);
    }

    public AuthUser admin(Permission permission) {
        AuthUser user = CurrentUser.get();
        if (!user.isAdmin()) {
            throw BusinessException.forbidden("Administrator access required");
        }
        CurrentUser.require(permission);
        return user;
    }

    public UserSummary summary(Long userId) {
        return identityClient.summary(userId);
    }

    public Map<Long, UserSummary> summaries(Collection<Long> ids) {
        try {
            return identityClient.summaries(ids);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private AuthUser onBehalfOf(Role role) {
        Long userId = CurrentUser.subjectId();
        UserSummary summary = identityClient.summary(userId);
        if (!role.name().equals(summary.role())) {
            throw BusinessException.badRequest("User " + userId + " is not a " + (role == Role.SELLER ? "service provider" : "purchaser"));
        }
        return new AuthUser(summary.id(), summary.name(), role, summary.status(), summary.referenceNo(), summary.permissions());
    }

    private void verify(AuthUser user, Permission permission, String label) {
        UserSummary summary = identityClient.summary(user.userId());
        if (!summary.active()) {
            throw BusinessException.forbidden("Your " + label + " account is " + summary.status().toLowerCase().replace('_', ' ')
                    + ". This action is restricted until the account is active.");
        }
        if (permission != null && (summary.permissions() == null || !summary.permissions().contains(permission.name()))) {
            throw BusinessException.forbidden("Privilege '" + permission.getDescription() + "' has not been granted to your account");
        }
    }
}
