package org.nuxeo.ecm.platform.ui.web.invalidations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 * Annotation used to mark method to be called by the interceptor to inject the
 * current DocumentModel
 *
 * @author tiry
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface DocumentContextInvalidation {

}
