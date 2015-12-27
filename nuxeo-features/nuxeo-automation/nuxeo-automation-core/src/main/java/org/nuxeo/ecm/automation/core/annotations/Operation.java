/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * <p>
 * An operation may provide an ID as the annotation value. If no id is specified the class name will be used as the ID.
 * <p>
 * The ID is the key used to register the operation.
 * <p>
 * Make sure you choose a proper ID name to avoid collisions (using the default: ID the class name can be a solution).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Operation {

    /**
     * The operation ID (mandatory).
     * <p>
     * If not specified the absolute name of the annotated class will be used.
     */
    String id() default "";

    /**
     * The operation category (optional), useful for documentation.
     * <p>
     * Provide a category to be used by the UI to classify the operations (on the documentation page or in Studio).
     */
    String category() default "Others";

    /**
     * The operation label (optional), useful for documentation.
     * <p>
     * Provide a label for the operation to be used in UI (should not contain any HTML code).
     */
    String label() default "";

    /**
     * Name of the context requires by this operation (optional), useful for documentation.
     * <p>
     * Provide the name of the context required by this operation. Example: event, ui, wf, etc..
     */
    String requires() default "";

    /**
     * Description of this operation (optional), useful for documentation.
     * <p>
     * Provide a description of the operation (may contain HTML code).
     */
    String description() default "";

    /**
     * Nuxeo version from which this operation is available (optional), useful for documentation.
     * <p>
     * The default value is the null string "" which means no specific version is required. Examples: "5.4", "5.9.1".
     */
    String since() default "";

    /**
     * Nuxeo version from which this operation is deprecated (optional), useful for documentation.
     * <p>
     * The default value is the null string "" which means no specific version. Examples: "5.4", "5.9.1".
     *
     * @since 5.9.1
     */
    String deprecatedSince() default "";

    /**
     * Boolean indicating if this operation should be exposed in Studio (optional), defaults to true.
     * <p>
     * This is convenient helper for Studio operations export.
     *
     * @since 5.9.1
     */
    boolean addToStudio() default true;

    /**
     * ID Aliases array for a given operation.
     *
     * @since 7.1
     */
    String[] aliases() default {};

}
