package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allow the usage of multiple @Deploy
 * <p>
 * Deployable artifacts are either bundles either components: Example
 *
 * <pre>
 * &#64;Deploy({"org.nuxeo.runtime", "org.nuxeo.core:OSGI-INF/component.xml"})
 * </pre>
 *
 * can now be written: *
 *
 * <pre>
 * &#64;Deploy("org.nuxeo.runtime")
 * &#64;Deploy("org.nuxeo.core:OSGI-INF/component.xml")
 * </pre>
 */

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Deploys {

    Deploy[] value();

}