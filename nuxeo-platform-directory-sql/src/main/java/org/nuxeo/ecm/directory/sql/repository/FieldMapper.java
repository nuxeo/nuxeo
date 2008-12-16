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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.sql.Types;

import org.nuxeo.ecm.directory.DirectoryException;

/**
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 *
 */
public class FieldMapper {

    // Utility class.
    private FieldMapper() {
    }

    public static int getSqlField(String name) throws DirectoryException {
        if (name.equals("integer")) {
            return Types.INTEGER;
        } else if (name.equals("long")) {
            return Types.INTEGER;
        } else if (name.equals("string")) {
            return Types.VARCHAR;
        } else if (name.equals("date")) {
            return Types.TIMESTAMP;
        } else {
            throw new DirectoryException("no SQL type mapping for: " + name);
        }
    }

}
