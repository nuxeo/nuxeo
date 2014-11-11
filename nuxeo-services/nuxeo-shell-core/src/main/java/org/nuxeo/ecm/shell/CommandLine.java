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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandLine extends ArrayList<Token> {

    private static final long serialVersionUID = -4057256104885351786L;

    private final Map<String,String> options;
    private final List<String> parameters;
    private final CommandLineService service;

    private String command;


    public CommandLine(CommandLineService service) {
        this.service = service;
        options = new HashMap<String, String>();
        parameters = new ArrayList<String>();
    }

    public String getCommand() {
        return command;
    }

    public Token addCommand(String command) {
        Token token = new Token(Token.COMMAND, command, size());
        add(token);
        this.command = command;
        return token;
    }

    public Token setCommand(String command) {
        Token token = new Token(Token.COMMAND, command, size());
        set(0, token);
        this.command = command;
        return token;
    }

    public boolean isInteractive() {
        return options.containsKey(Options.INTERACTIVE);
    }

    /**
     * Adds a new option as parsed from the command line.
     * <p>
     * This should be called by preserving the order as in raw command line. The
     * order is useful for auto-completion
     *
     * @param name
     */
    public Token addOption(String name) {
        Token token = new Token(Token.OPTION, name, size());
        add(token);
        options.put(name, "");
        return token;
    }

    /**
     * Adds a new token was parsed.
     */
    public Token addOptionValue(String name, String value) {
        Token token = new Token(Token.VALUE, value, size());
        for (Token t : this) {
            if (t.type == Token.OPTION && t.value.equals(name)) {
                token.info = t.index;
                t.info = token.index;
                break;
            }
        }
        add(token);
        options.put(name, value);
        return token;
    }

    /**
     * Adds a new Parameter token.
     */
    public Token addParameter(String str) {
        Token token = new Token(Token.PARAM, str, size(), parameters.size());
        add(token);
        parameters.add(str);
        return token;
    }

    public Token getToken(int offset) {
        return get(offset);
    }

    public boolean isOptionSet(String name) {
        return getOption(name) != null;
    }

    public String getOption(String name) {
        String value = options.get(name);
        if (value == null) {
            CommandOption opt = service.getCommandOption(name);
            if (opt != null) {
                return opt.getDefaultValue();
            }
        }
        return value;
    }

    public String[] getParameters() {
        return parameters.toArray(new String[parameters.size()]);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Command Line: ").append(command).append("\r\n");
        builder.append(options.toString());
        builder.append("\r\n").append(parameters.toString());
        return builder.toString();
    }

}
