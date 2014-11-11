/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.xmap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XMemberAnnotation(XMemberAnnotation.NODE_MAP)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface XNodeMap {

    /**
     * A path expression specifying the XML node to bind to.
     *
     * @return the node xpath
     */
    String value();

    /**
     * Whether to trim text content for element nodes.
     */
    boolean trim() default true;

    /**
     * The path relative to the current node
     * (which is located by {@link XNodeMap#value()}) which contain
     * the map key to be used.
     */
    String key();

    /**
     * The type of collection object.
     */
    Class type();

    /**
     * The type of the objects in this collection.
     *
     * @return the type of items
     */
    Class componentType();

    /**
     * Whether the container should be set to null when no value is specified.
     */
    boolean nullByDefault() default false;

}
