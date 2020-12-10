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

import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Annotation representing the registry to create for annotated {@link XObject} class.
 * <p>
 * Using this annotation will enable standard features for merge, enablement and removal, unless specified otherwise.
 * <p>
 * Override of the behaviour using {@link XMerge}, {@link XEnable} and {@link XRemove} annotations will be ignored
 * unless they are disabled.
 *
 * @since 11.5
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XRegistry {

    /**
     * Specifies if the default merge behavior should be applied, in which case merge will be specified thanks to node
     * {@link XMerge#MERGE}, and will be done by default.
     */
    boolean merge() default true;

    /**
     * Specifies if the default enablement behavior should be applied, in which case enablement will be specified thanks
     * to node {@link XEnable#ENABLE}, and will be done by default.
     */
    boolean enable() default true;

    /**
     * Specifies if the default removal behavior should be applied, in which case removal will be specified thanks to
     * node {@link XRemove#REMOVE}, and will not be done by default.
     */
    boolean remove() default true;

}
