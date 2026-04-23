package org.ujorm.petstore.utilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** URL mapping definition for servlets */
@Retention(RetentionPolicy.RUNTIME)
public @interface WebRoute {
    String value();
}