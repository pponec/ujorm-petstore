package org.ujorm.petstore.nativeimage;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Cuts the Java compiler out of the native image.
 * <p>
 * Two independent code paths reach {@link ToolProvider#getSystemJavaCompiler()}: the Ujorm
 * {@code ClassGenerator}, which is the fallback for a domain handler that was not pre-compiled,
 * and the H2 {@code SourceCompiler}, which compiles user defined Java functions. Neither is ever
 * taken by this application - the handlers are pre-compiled during the build and no Java function
 * is declared - but a static analysis can not know that, so it links the whole {@code jdk.compiler}
 * module in. That module was the second largest contributor to the image, right after
 * {@code java.base}.
 * <p>
 * The substitution reports that no compiler is available, which is exactly what a plain JRE does.
 * Ujorm already handles that answer and says so: <i>"Java Compiler unavailable ... pre-generate the
 * handler classes at build time"</i>.
 * <p>
 * The class is inert outside a native image: nothing loads it, and the annotations come from a
 * {@code provided} dependency that never reaches the WAR.
 */
@TargetClass(ToolProvider.class)
final class ToolProviderSubstitution {

    /** There is no Java compiler inside a native image. */
    @Substitute
    public static JavaCompiler getSystemJavaCompiler() {
        return null;
    }
}
