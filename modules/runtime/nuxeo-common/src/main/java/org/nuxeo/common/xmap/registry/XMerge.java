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
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Annotation representing the merge behavior for corresponding object or field.
 * <p>
 * If annotation is placed on a field annotated by {@link XNode}, this will be considered as the merge behavior for the
 * containing {@link XObject}. The value will be ignored and taken on the corresponding XNode instead.
 * <p>
 * If annotation is placed on a field annotated by {@link XNodeList} or {@link XNodeMap}, this will be considered as the
 * merge behavior for the list or map content.
 *
 * @since 11.5
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XMerge {

    public final String MERGE = "@merge";

    /**
     * The xpathy expression to retrieve the merge behavior.
     * <p>
     * Only processed when placed on a field annotated by {@link XNodeList} or {@link XNodeMap}.
     */
    String value() default MERGE;

    /**
     * The xpathy fallback expression to retrieve the merge behavior.
     * <p>
     * Only processed when placed on a field annotated by {@link XNodeList} or {@link XNodeMap}.
     *
     * @see XNode#fallback()
     */
    String fallback() default XNode.NO_FALLBACK_MARKER;

    /**
     * The default value for the behavior, if unspecified.
     *
     * @see XNode#defaultAssignment()
     */
    boolean defaultAssignment() default true;

}
