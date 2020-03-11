/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.reflect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;

/**
 * Annotation used to setup a class as a marshaller and define its instantiation mode and priority.
 * <p>
 * see {@link Instantiations} for instantiation mode explanation.
 * </p>
 * <p>
 * see {@link MarshallerRegistry} for instantiation rules.
 * </p>
 *
 * @since 7.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Setup {

    /**
     * see {@link Instantiations} for values.
     *
     * @since 7.2
     */
    Instantiations mode();

    /**
     * see {@link Priorities} for example values.
     *
     * @since 7.2
     */
    int priority() default Priorities.DEFAULT;

}
