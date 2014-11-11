/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.client.command;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandToken {

    public final static String COMMAND = "command";
    public final static String ANY = "*";
    public final static String FILE = "file";
    public final static String DOCUMENT = "document";

    public String[] names;
    public String valueType; // null | string | command | file | doc
    public String defaultValue;
    public boolean isArgument;
    public boolean isOptional;


    public boolean isValueRequired() {
        return valueType != COMMAND && valueType != null && !isArgument;
    }

    public boolean isCommand() {
        return valueType == COMMAND;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public boolean isFlag() {
        return valueType == null;
    }

    /**
     * @return the isArgument.
     */
    public boolean isArgument() {
        return isArgument;
    }

    /**
     * @return the defaultValue.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the valueType.
     */
    public String getValueType() {
        return valueType;
    }

    public String getName() {
        return names[0];
    }

    /**
     * @return the names.
     */
    public String[] getNames() {
        return names;
    }

}
