/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Marks a class as being an operation.
 * An operation may provide an ID as the annotation value. If no id is specified
 * the class name will be used as the ID.
 * The ID is the key used to register the operation. 
 * Make sure you choose a proper ID name to avoid collisions. (using the default: ID the class name can be a solution).    
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Operation {

    /**
     * The operation ID. If not specified the absolute name of the annotated class will be used.
     * @return
     */
    String id() default "";
    
    /**
     * Optional attribute - useful to generate operation documentation.
     *
     * Provide a category to be used by the UI to classify the operations.
     *  
     * @return
     */
    String category() default "Others";

    /**
     * Optional attribute - useful to generate operation documentation.
     *
     * Provide a label for the operation to be used in UI. (should not contain HTML code)
     * 
     * @return
     */
    String label() default "";
    
    /**
     * Optional attribute - useful to generate operation documentation.
     *
     * Provide the name of the context required by this operation. Example: event, ui, wf etc
     * 
     * @return
     */
    String requires() default "";

    /**
     * Optional attribute - useful to generate operation documentation.
     *
     * Provide a description of the operation. (may contain HTML code)
     * @return
     */
    String description() default "";

}
