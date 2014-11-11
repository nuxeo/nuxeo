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

package org.nuxeo.common.xmap;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface XAccessor {

    /**
     * Gets the type of the object to be set by this setter.
     *
     * @return the setter object type
     */
    Class getType();

    /**
     * Sets the value of the underlying member.
     *
     * @param instance the instance of the object that owns this field
     * @param value the value to set
     * @throws Exception
     */
    void setValue(Object instance, Object value)
            throws Exception;


    Object getValue(Object instance) throws Exception;

}
