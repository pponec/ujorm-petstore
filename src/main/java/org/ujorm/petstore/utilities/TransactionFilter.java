package org.ujorm.petstore.utilities;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.*;

import java.io.IOException;

/**
 * Filter that wraps every HTTP request in a database transaction.
 * <br/>
 * NOTE: Annotation {@link jakarta.servlet.annotation.WebFilter} supports non-argument constructors only.
 * @see TransactionFilter Registraton is there.
 */
@Singleton // This is crucial for Avaje to see this class as a Bean
public class TransactionFilter implements Filter {

    private final TransactionManager tm;

    @Inject
    public TransactionFilter(TransactionManager tm) {
        this.tm = tm;
    }

    /**
     * Executes the filter chain within a database transaction.
     * The try-catch block is necessary because the TransactionManager wraps
     * checked exceptions (IOException, ServletException) into a RuntimeException.
     * To ensure the servlet container handles these errors correctly, we must
     * unwrap and rethrow the original checked exceptions.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain
    ) throws IOException, ServletException {
        try {
            tm.run(() -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (RuntimeException e) {
            var cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else if (cause instanceof ServletException) {
                throw (ServletException) cause;
            }
            throw e;
        }
    }
}