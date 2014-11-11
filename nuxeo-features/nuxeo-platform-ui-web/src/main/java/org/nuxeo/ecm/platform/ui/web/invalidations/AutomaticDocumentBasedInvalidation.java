package org.nuxeo.ecm.platform.ui.web.invalidations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.interceptor.Interceptors;

/**
 * Annotation for Seam components that will use the automatic
 * Document based invalidation system.
 *
 * On each call, the currentDocument will be passed to a invalidation method
 * (this method must be annotated with @DocumentContextInvalidation)
 *
 * @author tiry
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
@Interceptors(DocumentContextInvalidatorInterceptor.class)
public @interface AutomaticDocumentBasedInvalidation {

}
