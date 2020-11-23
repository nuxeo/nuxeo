/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.common.xmap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to map XML nodes.
 */
@XMemberAnnotation(XMemberAnnotation.NODE)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface XNode {

    /**
     * An xpathy expression specifying the XML node to bind to.
     *
     * @return the node xpath
     */
    String value() default "";

    /**
     * Marker for fallback to be ignored.
     *
     * @since 11.5
     */
    public static final String NO_FALLBACK_MARKER = "__NO_FALLBACK_MARKER__";

    /**
     * An xpathy expression specifying the fallback XML node to bind to.
     * <p>
     * Useful for XML format evolutions: provides compatibility on previous format.
     *
     * @since 11.5
     */
    String fallback() default NO_DEFAULT_ASSIGNMENT_MARKER;

    /**
     * Marker for default assignment to be ignored.
     *
     * @since 11.5
     */
    public static final String NO_DEFAULT_ASSIGNMENT_MARKER = "__NO_DEFAULT_ASSIGNMENT_MARKER__";

    /**
     * String representation of the default assignment for the retrieved value for this node.
     * <p>
     * The corresponding value will be converted, and will be used as a default value in case the node path and its
     * fallback are not present on the XML representation.
     *
     * @since 11.5
     */
    String defaultAssignment() default NO_DEFAULT_ASSIGNMENT_MARKER;

    /**
     * Whether to trim text content for element nodes.
     * <p>
     * Ignored for attribute nodes.
     */
    boolean trim() default true;

}
