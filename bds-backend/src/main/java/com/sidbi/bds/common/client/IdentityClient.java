package com.sidbi.bds.common.client;

import com.sidbi.bds.auth.service.InternalIdentityService;
import com.sidbi.bds.common.client.dto.RatingUpdate;
import com.sidbi.bds.common.client.dto.UserContact;
import com.sidbi.bds.common.client.dto.UserSummary;
import com.sidbi.bds.common.security.Permission;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IdentityClient {

    private final ObjectProvider<InternalIdentityService> service;
    private final ModuleCalls calls;

    public IdentityClient(ObjectProvider<InternalIdentityService> service, ModuleCalls calls) {
        this.service = service;
        this.calls = calls;
    }

    public UserSummary summary(Long userId) {
        return calls.call(() -> service.getObject().summary(userId));
    }

    public Map<Long, UserSummary> summaries(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream().distinct().toList();
        List<UserSummary> list = calls.call(() -> service.getObject().summaries(ids));
        return list == null ? Map.of() : list.stream().collect(Collectors.toMap(UserSummary::id, Function.identity(), (a, b) -> a));
    }

    public List<Long> usersWithPermission(String permission) {
        return calls.call(() -> service.getObject().withPermission(Permission.valueOf(permission)));
    }

    public UserSummary lookupByEmail(String email) {
        return calls.call(() -> service.getObject().lookupByEmail(email));
    }

    public UserContact contact(Long userId) {
        return calls.call(() -> service.getObject().contact(userId));
    }

    public void updateSellerRating(Long sellerId, RatingUpdate update) {
        calls.run(() -> service.getObject().updateRating(sellerId, update));
    }
}
