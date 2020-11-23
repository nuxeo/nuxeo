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
package org.nuxeo.common.xmap.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodes;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Annotation representing the id to be retrieved for registry identification.
 * <p>
 * If annotation is placed on a field annotated by {@link XNode}, this field will be considered as the id for the
 * containing {@link XObject}. This annotation value will be ignored and taken on the corresponding XNode instead.
 *
 * @since 11.5
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface XRegistryId {

    String ID = "@id";

    String NAME = "@name";

    /**
     * The xpathy expression to retrieve the identifier.
     * <p>
     * Multiple values can be used for aggregation.
     * <p>
     * Only processed when placed on a type annotated by {@link XObject}.
     */
    String[] value() default ID;

    /**
     * The xpathy fallback expression to retrieve the identifier.
     * <p>
     * Only processed when placed on a type annotated by {@link XObject}.
     *
     * @see XNode#fallback()
     */
    String fallback() default NAME;

    /**
     * The default value for this identifier.
     * <p>
     * Only processed when placed on a type annotated by {@link XObject}.
     *
     * @see XNodes#defaultAssignment()
     */
    String defaultAssignment() default XNode.NO_DEFAULT_ASSIGNMENT_MARKER;

    /**
     * The default value for this identifier.
     * <p>
     * Only processed when placed on a type annotated by {@link XObject} and when multiple values are defined for
     * aggregation.
     *
     * @see XNodes#separator()
     */
    String separator() default ":";

}
