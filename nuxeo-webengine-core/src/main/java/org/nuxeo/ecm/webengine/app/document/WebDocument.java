package org.nuxeo.ecm.webengine.app.document;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by JAX-RS resources that expose core documents.
 *
 * @author bstefanescu
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebDocument {

    /**
     * The type of the document to expose
     * @return
     */
    String value();

}
