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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.client.cmds.Exit;
import org.nuxeo.ecm.webengine.client.cmds.Help;
import org.nuxeo.ecm.webengine.client.cmds.Test;
import org.nuxeo.ecm.webengine.client.util.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandRegistry {

    protected static CommandRegistry  builtinCommands = new CommandRegistry(null);
    protected Map<String, Command> commands;

    static {
        builtinCommands.registerCommand(new Help());
        builtinCommands.registerCommand(new Exit());
        builtinCommands.registerCommand(new Test());
    }
    
    
    public CommandRegistry() {
        this (builtinCommands.commands);
    }
    
    public CommandRegistry(Map<String, Command> cmds) {
        commands = new HashMap<String, Command>();
        if (cmds != null) {
            commands.putAll(cmds);
        }
    }
    
    public void registerCommand(Command cmd) {
        for (String alias : cmd.getAliases()) {
            commands.put(alias, cmd);
        }
    }
    
    public void unregisterCommand(String name) {
        Command cmd = commands.remove(name);
        if (cmd != null) {
            for (String alias : cmd.getAliases()) {
                commands.remove(alias);
            }
        }
    }
    
    public String[] getCommandNames() {
        return commands.keySet().toArray(new String[commands.size()]);
    }
    
    public Command[] getCommands() {
        return commands.values().toArray(new Command[commands.size()]);
    }
    
    public Command getCommand(String name) {
        return commands.get(name);
    }

    public String[] getCompletionInfo(String line, int offset) {
        if (offset == -1) {
            offset = line.length();
        }
        // get the word containing the offset
        int i = offset-1;
        while (i >=0) {
            char c = line.charAt(i);
            if (Character.isWhitespace(c)) {
                break;
            }
            i--;
        }        
        String word = offset > i && i>=0 ? line.substring(i+1, offset) : ""; 
        String prefix = line.substring(0, i).trim();
        if (prefix.length() == 0) {
           return new String[] {CommandToken.COMMAND, word}; 
        }
        String[] segments = StringUtils.tokenize(prefix);
        String cmdName = segments[0];
        Command cmd = getCommand(cmdName);
        if (cmd == null) {
            return null;
        }
        i = segments.length-1;
        CommandToken token = cmd.syntax.getToken(segments[i]);
        if (token == null) {
            i--;
            int k = 0;
            while (i > 0) {
                token = cmd.syntax.getToken(segments[i]);
                if (token != null) {
                    
                }
                 i--;
                 k++;
             }
            token = cmd.syntax.getArgument(k);
        }
        if (token == null) {
            return null;
        }
        return new String[] {token.valueType, word} ;
    }
    
    
    public static CommandRegistry getBuiltinCommands() {
        return builtinCommands;
    }
    
    public static Command getBuiltinCommand(String name) {
        return builtinCommands.getCommand(name);
    }
    
    public static void registerBuiltinCommand(Command cmd) {
        builtinCommands.registerCommand(cmd);
    }

    public static void unregisterBuiltinCommand(Command cmd) {
        builtinCommands.unregisterCommand(cmd.getName());
    }


}
