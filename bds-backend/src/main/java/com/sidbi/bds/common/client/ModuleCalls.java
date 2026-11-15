package com.sidbi.bds.common.client;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Runs a cross-module call in its own transaction so a failure in the called module
 * cannot mark the caller's transaction rollback-only (callers often swallow these failures).
 */
@Component
public class ModuleCalls {

    private final TransactionTemplate template;

    public ModuleCalls(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
        this.template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T call(Supplier<T> action) {
        return template.execute(status -> action.get());
    }

    public void run(Runnable action) {
        template.executeWithoutResult(status -> action.run());
    }
}
