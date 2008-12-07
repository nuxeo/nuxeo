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
import java.util.List;

import org.nuxeo.ecm.webengine.client.util.StringUtils;

/**
 *
 * cmd [-opt|-o:type?defValue] [name:type]
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandSyntax {

    protected List<CommandToken> tokens = new ArrayList<CommandToken>();
    protected List<CommandToken> args = new ArrayList<CommandToken>();
    protected HashMap<String, CommandToken> map = new HashMap<String, CommandToken>();


    public CommandToken getCommandToken() {
        return tokens.get(0);
    }

    public List<CommandToken> getArguments() {
        return args;
    }

    public CommandToken getArgument(int index) {
        if (index >=args.size()) {
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
     * Get all parameter keys
     * @return
     */
    public String[] getParameterKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        for (int i=1,len=tokens.size(); i<len; i++) { // skip first token
            CommandToken token = tokens.get(i);
            if (!token.isArgument()) {
                for (String key : token.getNames()) {
                    keys.add(key);
                }
            }
        }
        return keys.toArray(new String[keys.size()]);
    }

    public void addToken(CommandToken tok) {
        tokens.add(tok);
        for (int i=0; i<tok.names.length; i++) {
            map.put(tok.names[i], tok);
        }
        if (tok.isArgument) {
            args.add(tok);
        }
    }

    public static CommandToken parseToken(String text) {
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

    public static CommandSyntax parse(String text) {
        String[] toks = StringUtils.tokenize(text);
        if (toks.length == 0) {
            throw new IllegalArgumentException("cannot parse empty command lines");
        }
        CommandSyntax syntax = new CommandSyntax();
        if (toks.length == 0) {
            return syntax;
        }
        CommandToken tok = new CommandToken();
        tok.names = StringUtils.split(toks[0], '|', false);
        tok.valueType = CommandToken.COMMAND;
        syntax.addToken(tok);
        for (int i=1; i<toks.length; i++) {
            tok = parseToken(toks[i]);
            syntax.addToken(tok);
        }
        return syntax;
    }



}
