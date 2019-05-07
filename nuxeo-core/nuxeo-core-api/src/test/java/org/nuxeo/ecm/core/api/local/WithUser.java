package org.nuxeo.ecm.core.api.local;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Interface allowing to login as a specific user into Nuxeo Platform.
 * <p>
 * It leverages {@link DummyLoginAs} class and so log in as {@code Administrator} or {@code anonymous} will give
 * specific roles.
 *
 * @since 11.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface WithUser {

    /**
     * @return the username to use to login
     */
    @SuppressWarnings("deprecation")
    String value() default SecurityConstants.ADMINISTRATOR;
}
