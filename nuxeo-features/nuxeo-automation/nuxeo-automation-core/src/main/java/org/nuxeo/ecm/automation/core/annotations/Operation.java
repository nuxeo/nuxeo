/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as being an operation. An operation may provide an ID as the
 * annotation value. If no id is specified the class name will be used as the
 * ID. The ID is the key used to register the operation. Make sure you choose a
 * proper ID name to avoid collisions. (using the default: ID the class name
 * can be a solution).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Operation {

    /**
     * The operation ID. If not specified the absolute name of the annotated
     * class will be used.
     */
    String id() default "";

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * a category to be used by the UI to classify the operations.
     */
    String category() default "Others";

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * a label for the operation to be used in UI. (should not contain HTML
     * code).
     */
    String label() default "";

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * the name of the context required by this operation. Example: event, ui,
     * wf, etc.
     */
    String requires() default "";

    /**
     * Optional attribute - useful to generate operation documentation. Provide
     * a description of the operation. (may contain HTML code)
     */
    String description() default "";

    /**
     * Optional attribute - indicate from which nuxeo version the operation is
     * available. The default value is the null string "" which means no
     * specific version is required.
     */
    String since() default "";

}
