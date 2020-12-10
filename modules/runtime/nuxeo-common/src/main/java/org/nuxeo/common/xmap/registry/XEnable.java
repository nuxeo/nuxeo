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
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Annotation to specify enablement behavior for corresponding object or field.
 * <p>
 * If annotation is placed on a field annotated by {@link XNode}, this will be considered as the enablement behavior for
 * the containing {@link XObject}.
 * <p>
 * The difference between removal thanks to {@link XRemove} usage and removal is that enablement will preserve existing
 * values for merge.
 * <p>
 * When an object is disabled, it will not be available to getter API, but re-enablement will preserve existing values.
 *
 * @since 11.5
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XEnable {

    public final String ENABLE = "@enable";

    /**
     * The default value for the behavior, if unspecified.
     *
     * @see XNode#defaultAssignment()
     */
    boolean defaultAssignment() default true;

}
