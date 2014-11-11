/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.Completor;
import jline.SimpleCompletor;

import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public abstract class AbstractCommandType implements CommandType {

    protected Class<? extends Runnable> cmdClass;

    protected List<Setter> injectable;

    protected Map<String, Token> params;

    protected List<Token> args;

    public AbstractCommandType(Class<? extends Runnable> cmdClass,
            List<Setter> injectable, Map<String, Token> params, List<Token> args) {
        this.cmdClass = cmdClass;
        this.params = params == null ? new HashMap<String, Token>() : params;
        this.args = args == null ? new ArrayList<Token>() : args;
        this.injectable = injectable == null ? new ArrayList<Setter>()
                : injectable;
    }

    public Class<?> getCommandClass() {
        return cmdClass;
    }

    public List<Token> getArguments() {
        return args;
    }

    public Map<String, Token> getParameters() {
        return params;
    }

    public String getSyntax() {
        ArrayList<String> argNames = new ArrayList<String>();
        for (Token arg : args) {
            if (arg.isArgument()) {
                if (arg.isRequired) {
                    argNames.add(arg.name);
                } else {
                    argNames.add("[" + arg.name + "]");
                }
            }
        }
        StringBuilder buf = new StringBuilder();
        buf.append(getName());
        if (!params.isEmpty()) {
            buf.append(" [options]");
        }
        for (String name : argNames) {
            buf.append(" ").append(name);
        }
        return buf.toString();
    }

    public Runnable newInstance(Shell shell, String... line)
            throws ShellException {
        Runnable cmd;
        try {
            cmd = createInstance(shell);
        } catch (Throwable t) {
            throw new ShellException(t);
        }
        inject(shell, cmd, line);
        return cmd;
    }

    protected Runnable createInstance(Shell shell) throws Exception {
        return cmdClass.newInstance();
    }

    /**
     * The last element in line must be an empty element (e.g. "") if you need
     * the next argument that may match. If the last element is not empty then
     * this element will be returned as an argument or parameter.
     * 
     * @param line
     * @return
     */
    protected Token getLastToken(String... line) {
        int index = -1;
        Token last = null;
        if (params != null) {
            for (int i = 1; i < line.length; i++) {
                String key = line[i];
                if (key.startsWith("-")) { // a param
                    Token arg = params.get(key);
                    if (arg != null && arg.isRequired) {
                        i++;
                        last = arg;
                        continue;
                    }
                } else { // an arg
                    last = null;
                    index++;
                }
            }
        }
        if (last != null) {
            return last;
        }
        if (index == -1 || index >= args.size()) {
            return null;
        }
        if (args != null) {
            return args.get(index);
        }
        return null;
    }

    protected Completor getParamCompletor(String prefix) {
        ArrayList<String> result = new ArrayList<String>();
        for (String key : params.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result.isEmpty() ? null : new SimpleCompletor(
                result.toArray(new String[result.size()]));
    }

    public Completor getLastTokenCompletor(Shell shell, String... line) {
        // check first for a param key completor
        String last = line[line.length - 1];
        if (last.startsWith("-")) { // may be a param
            Completor c = getParamCompletor(last);
            if (c != null) {
                return c;
            }
        }
        // check now for a value completor
        Token arg = getLastToken(line);
        if (arg == null) {
            return null;
        }
        if (arg.completor != null && !arg.completor.isInterface()) {
            try {
                return arg.completor.newInstance();
            } catch (Throwable t) {
                throw new ShellException("Failed to load completor: "
                        + arg.completor, t);
            }
        }
        return shell.getCompletorProvider().getCompletor(shell, this,
                arg.setter.getType());
    }

    protected void inject(Shell shell, Runnable cmd, String... line)
            throws ShellException {
        for (Setter s : injectable) {
            s.set(cmd, shell.getContextObject(s.getType()));
        }
        int index = 0;
        int argCount = args.size();
        for (int i = 1; i < line.length; i++) {
            String key = line[i];
            if (key.startsWith("-")) {
                Token arg = params.get(key);
                if (arg == null) {
                    throw new ShellException("Unknown parameter: " + key);
                }
                String v = null;
                if (!arg.isRequired) {
                    v = "true";
                } else if (i == line.length - 1) {
                    throw new ShellException("Parameter " + key
                            + " must have a value");
                } else {
                    v = line[++i];
                }
                arg.setter.set(
                        cmd,
                        shell.getValueAdapter().getValue(shell,
                                arg.setter.getType(), v));
            } else {
                if (index >= argCount) {
                    throw new ShellException("Too many arguments");
                }
                Token arg = args.get(index++);
                arg.setter.set(
                        cmd,
                        shell.getValueAdapter().getValue(shell,
                                arg.setter.getType(), key));
            }
        }
        for (int i = index; i < argCount; i++) {
            if (args.get(i).isRequired) {
                throw new ShellException("Required argument "
                        + args.get(i).name + " is missing");
            }
        }
    }

    public int compareTo(CommandType o) {
        return getName().compareTo(o.getName());
    }
}
