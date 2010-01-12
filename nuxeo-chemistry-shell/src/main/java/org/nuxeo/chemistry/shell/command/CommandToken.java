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

package org.nuxeo.chemistry.shell.command;

import org.nuxeo.chemistry.shell.util.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandToken {

    public static final String COMMAND = "command";
    public static final String ANY = "*";
    public static final String FILE = "file";
    public static final String DOCUMENT = "document";

    private String[] names;
    private String valueType; // null | string | command | file | doc
    private String defaultValue;
    private boolean isArgument;
    private boolean isOptional;

    public static CommandToken parseCommand(String text) {
        CommandToken tok = new CommandToken();
        tok.names = StringUtils.split(text, '|', false);
        tok.valueType = COMMAND;
        return tok;
    }

    public static CommandToken parseArg(String text) {
        CommandToken tok = new CommandToken();
        if (text.startsWith("[")) {
            tok.isOptional = true;
            text = text.substring(1, text.length()-1);
        }
        int p = text.indexOf(':');
        if (p > -1) {
            tok.valueType = text.substring(p+1);
            text = text.substring(0, p);
            p = tok.valueType.indexOf('?');
            if (p > -1) {
                tok.defaultValue = tok.valueType.substring(p+1);
                tok.valueType = tok.valueType.substring(0, p);
            }
        }
        // parse names in text
        tok.names = StringUtils.split(text, '|', true);
        tok.isArgument = !tok.names[0].startsWith("-");
        return tok;
    }

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
        return names[0].startsWith("-");
    }

    public boolean isArgument() {
        return isArgument;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getValueType() {
        return valueType;
    }

    public String getName() {
        return names[0];
    }

    public String[] getNames() {
        return names;
    }

}
