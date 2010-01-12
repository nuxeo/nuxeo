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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.chemistry.shell.console.CompositeCompletor;
import org.nuxeo.chemistry.shell.util.StringUtils;

/**
 * cmd [-opt|-o:type?defValue] [name:type]
 *
 * Supported types: file, dir, command, item. See {@link CompositeCompletor}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CommandSyntax {

    protected final List<CommandToken> tokens = new ArrayList<CommandToken>();
    protected final List<CommandToken> args = new ArrayList<CommandToken>();
    protected final HashMap<String, CommandToken> map = new HashMap<String, CommandToken>();

    /**
     * Static factory.
     */
    public static CommandSyntax parse(String text) {
        String[] tokens = StringUtils.tokenize(text);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("cannot parse empty command lines");
        }
        CommandSyntax syntax = new CommandSyntax();
        if (tokens.length == 0) {
            return syntax;
        }
        CommandToken tok = CommandToken.parseCommand(tokens[0]);
        syntax.addToken(tok);
        for (int i=1; i<tokens.length; i++) {
            tok = CommandToken.parseArg(tokens[i]);
            syntax.addToken(tok);
        }
        return syntax;
    }

    public CommandToken getCommandToken() {
        return tokens.get(0);
    }

    public List<CommandToken> getArguments() {
        return args;
    }

    public CommandToken getArgument(int index) {
        if (index >= args.size()) {
            return null;
        }
        return args.get(index);
    }

    public List<CommandToken> getTokens() {
        return tokens;
    }

    public CommandToken getToken(int i) {
        return tokens.get(i);
    }

    public CommandToken getToken(String key) {
        return map.get(key);
    }

    /**
     * Gets all parameter keys.
     */
    public String[] getParameterKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        for (int i=1,len=tokens.size(); i<len; i++) { // skip first token
            CommandToken token = tokens.get(i);
            if (!token.isArgument()) {
                keys.addAll(Arrays.asList(token.getNames()));
            }
        }
        return keys.toArray(new String[keys.size()]);
    }

    public void addToken(CommandToken tok) {
        tokens.add(tok);
        for (int i=0; i<tok.getNames().length; i++) {
            map.put(tok.getNames()[i], tok);
        }
        if (tok.isArgument()) {
            args.add(tok);
        }
    }

}
