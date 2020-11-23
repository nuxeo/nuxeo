/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Handles multiple nodes aggregation using a separator.
 * <p>
 * Resutling value should be a {@link String}.
 *
 * @since 11.5
 */
@XMemberAnnotation(XMemberAnnotation.NODES)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface XNodes {

    /**
     * An array of xpathy expressions specifying the XML nodes to bind to.
     */
    String[] values() default "";

    /**
     * String representation of the default assignment for the retrieved value for these nodes.
     *
     * @see XNode#defaultAssignment()
     */
    String defaultAssignment() default XNode.NO_DEFAULT_ASSIGNMENT_MARKER;

    /**
     * String separator to be used to aggregate the retrieved values.
     */
    String separator() default ":";

}
