/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list of artifacts to be deployed.
 * <p>
 * Deployable artifacts are either bundles either components:
 * <ul>
 * <li> A bundle entry is represented by the bundle symbolic name.
 * <li> A component entry is represented by an URI of the form: symbolicName:componentXmlPath,
 * where symbolicName is the symbolic name of the bundle owning the component.
 * </ul>
 * Example with one module:
 * <pre>
 * @Deploy("org.nuxeo.runtime")
 * </pre>
 * Example with several modules:
 * <pre>
 * @Deploy({"org.nuxeo.runtime", "org.nuxeo.core:OSGI-INF/component.xml"})
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.METHOD })
public @interface Deploy {
    /**
     * The artifact ID (symbolic name or bundle resource URI).
     */
    String[] value();

}
