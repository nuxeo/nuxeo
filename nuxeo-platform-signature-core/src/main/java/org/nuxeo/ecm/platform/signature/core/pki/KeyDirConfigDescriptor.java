/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Wojciech Sulejman
 */

package org.nuxeo.ecm.platform.signature.core.pki;

/**
 * Configuration for the directory used for key/certificate storage
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class KeyDirConfigDescriptor {

    private static String directoryName;

    private static String schemaName;

    private static String fieldName;

    public static String getDirectoryName() {
        return directoryName;
    }

    public static void setDirectoryName(String directoryName) {
        KeyDirConfigDescriptor.directoryName = directoryName;
    }

    public static String getSchemaName() {
        return schemaName;
    }

    public static void setSchemaName(String schemaName) {
        KeyDirConfigDescriptor.schemaName = schemaName;
    }

    public static String getFieldName() {
        return fieldName;
    }

    public static void setFieldName(String fieldName) {
        KeyDirConfigDescriptor.fieldName = fieldName;
    }

}
