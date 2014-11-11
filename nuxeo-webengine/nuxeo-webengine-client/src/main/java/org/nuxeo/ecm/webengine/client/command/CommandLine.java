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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.client.Client;
import org.nuxeo.ecm.webengine.client.Console;
import org.nuxeo.ecm.webengine.client.util.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandLine {

    protected List<CommandParameter> params;
    protected Map<String, CommandParameter> map;
    protected Command cmd;

    public CommandLine(CommandRegistry registry, String cmd) throws CommandException {
        this (registry, StringUtils.tokenize(cmd));
    }

    public CommandLine(CommandRegistry registry, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new IllegalArgumentException("CommandLine cannot be empty");
        }
        this.map = new HashMap<String, CommandParameter>();
        this.params = new ArrayList<CommandParameter>();
        cmd = registry.getCommand(args[0]);
        if (cmd == null) {
            throw new NoSuchCommandException(args[0]);
        }
        // build params
        CommandParameter param = new CommandParameter(args[0], cmd.syntax.tokens.get(0));
        params.add(param);
        int k = 0;
        for (int i=1; i<args.length; i++) {
            String key = args[i];
            if (param != null && param.token.isValueRequired()) {
                param.value = key;
                param = null;
            } else {
                CommandToken token = cmd.syntax.getToken(key);
                if (token == null) {
                    token = cmd.syntax.getArgument(k++);
                    if (token == null) {
                        throw new CommandSyntaxException(cmd, "Syntax Error: Extra argument found on position "+i);
                    }
                }
                if (token.isArgument()) {
                    param = new CommandParameter(token.getName(), token);
                    param.value = key;
                } else {
                    param = new CommandParameter(key, token);
                }
                params.add(param);
                map.put(param.token.getName(), param);
            }
        }
        // check if the last parameter has a value if it requires it
        if (param != null && param.getValue() == null && param.token.isValueRequired()) {
            throw new CommandSyntaxException(cmd, "Syntax Error: Value for parameter "+param.key+" is required");
        }
        // check if all required options are present - ignore first token which is the command token
        for (int i=1, len=cmd.syntax.tokens.size(); i<len; i++) {
            CommandToken token = cmd.syntax.tokens.get(i);
            if (!token.isOptional()) {
                if (!map.containsKey(token.getName())) {
                    throw new CommandSyntaxException(cmd, "Syntax Error: Missing required parameter: "+token.getName());
                }
            }
        }
    }

    public CommandParameter getLastParameter() {
        if (params.isEmpty()) {
            return null;
        }
        return params.get(params.size()-1);
    }

    public List<CommandParameter> getParameters() {
        return params;
    }

    public List<CommandParameter> getArguments() {
        ArrayList<CommandParameter> result = new ArrayList<CommandParameter>();
        for (CommandParameter arg : params) {
            if (arg.token.isArgument) {
                result.add(arg);
            }
        }
        return result;
    }

    public CommandParameter getParameter(String key) {
        return this.map.get(key);
    }

    public Command getCommand() throws Exception {
        return cmd;
    }

    public void run(Client client) throws Exception {
        getCommand().run(client, this);
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (CommandParameter param : params) {
            buf.append(param.key).append(" ");
            if (param.token.isValueRequired()) {
                String value = param.getValue();
                if (value != null) buf.append(value).append(" ");
            }
        }
        buf.setLength(buf.length()-1);
        return buf.toString();
    }

    
    public Map<String, Object> toMap() {
        // preserve params order
        LinkedHashMap<String, Object> args = new LinkedHashMap<String, Object>();
        int k = 0;
        for (CommandParameter param : params) {
            String key = param.getKey();
            String value = param.getValue();
            Object val = value;
            if (key == null) {
                key = "_"+(k++);
            }
            if (CommandToken.FILE.equals(param.token.valueType)) {
                val = Console.getDefault().getClient().getFile(value);                
            }
            args.put(key, val);
        }
        return args;
    }

}
