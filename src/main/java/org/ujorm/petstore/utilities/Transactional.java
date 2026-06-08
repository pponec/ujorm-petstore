package org.ujorm.petstore.utilities;

import io.avaje.inject.aop.Aspect;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative transaction boundary — a lightweight equivalent of Spring's
 * {@code org.springframework.transaction.annotation.Transactional}.
 * <p>
 *   A method (or every public method of a type) annotated with {@code @Transactional}
 *   is woven by Avaje at compile time and executed inside a single JDBC transaction
 *   managed by {@link TransactionalAspect} / {@link TransactionManager}. The transaction
 *   is committed when the method returns normally and rolled back on any exception.
 * </p>
 * <p>
 *   Propagation is {@code REQUIRED}: a {@code @Transactional} method called from within
 *   another active transaction joins it (a single commit at the outermost boundary).
 *   As with Spring, the aspect only applies when the bean is invoked <i>through the proxy</i>,
 *   so self-invocation ({@code this.otherMethod()}) does not start a new transaction.
 * </p>
 *
 * @see TransactionalAspect
 * @see TransactionManager
 */
@Aspect
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {

    /**
     * Hint that the transaction does not modify data. The underlying JDBC connection
     * is flagged {@link java.sql.Connection#setReadOnly(boolean) read-only}, which lets
     * the driver/pool optimise the work (and documents intent at the call site).
     */
    boolean readOnly() default false;
}
