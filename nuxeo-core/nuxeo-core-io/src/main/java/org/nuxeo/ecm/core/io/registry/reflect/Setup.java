/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
