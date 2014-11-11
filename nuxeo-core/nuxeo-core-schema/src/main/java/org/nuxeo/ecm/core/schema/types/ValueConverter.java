/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.schema.types;

import java.io.Serializable;

/**
 * Converts a value to an object compatible with the associated type.
 * <p>
 * A value converter is associated with a type and enable the programmer
 * to change the default java types corresponding to nuxeo types
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated use {@link TypeHelper} instead
 */
@Deprecated
public interface ValueConverter extends Serializable {

    /**
     * Converts the given value to an object compatible with the associated type.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws TypeException if the value to convert is not compatible with the associated type
     */
    Object convert(Object value) throws TypeException;

}
