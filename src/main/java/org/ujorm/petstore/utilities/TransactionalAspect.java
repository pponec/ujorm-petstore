package org.ujorm.petstore.utilities;

import io.avaje.inject.aop.AspectProvider;
import io.avaje.inject.aop.Invocation;
import io.avaje.inject.aop.MethodInterceptor;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;

/**
 * Avaje aspect that turns {@link Transactional} into a real transaction boundary.
 * <p>
 *   For every intercepted call it delegates to {@link TransactionManager#run(boolean, TransactionManager.SupplierThrowing)},
 *   which opens a connection (or joins the current one), commits on success and rolls
 *   back on failure. This moves the transaction demarcation from the HTTP layer
 *   (a per-request servlet filter) down to the service layer — the Spring Boot model.
 * </p>
 */
@Singleton
public class TransactionalAspect implements AspectProvider<Transactional> {

    private final TransactionManager tm;

    public TransactionalAspect(TransactionManager tm) {
        this.tm = tm;
    }

    @Override
    public MethodInterceptor interceptor(Method method, Transactional transactional) {
        var readOnly = transactional.readOnly();
        return invocation -> tm.run(readOnly, () -> proceed(invocation));
    }

    /**
     * Proceeds with the intercepted method call, adapting its {@code Throwable} contract
     * to the {@code Exception}-based task expected by {@link TransactionManager}.
     * The result is captured by {@link Invocation} itself, so the proxy returns it.
     */
    private static Object proceed(Invocation invocation) throws Exception {
        try {
            return invocation.invoke();
        } catch (Exception | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Transactional invocation failed", t);
        }
    }
}
