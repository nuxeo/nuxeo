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

/**
 * Annotation representing the merge behavior for corresponding object or field.
 * <p>
 * If annotation is placed on a field annotated by {@link @XNode}, this will be considered as the merge behavior for
 * containing {@link @XObject}. Value will be ignore and taken on the corresponding XNode instead.
 * <p>
 * If annotation is placed on a field annotated by {@link @XNodeList} or {@link @XNodeMap}, this will be considered as
 * the merge behavior for the list or map.
 *
 * @since TODO
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XMerge {

    public final String MERGE = "@merge";

    String value() default MERGE;

    String fallbackValue() default XNode.NO_FALLBACK_VALUE_MARKER;

    boolean defaultValue() default true;

}
