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

package org.nuxeo.ecm.shell.commands;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.Completor;
import jline.ConsoleReader;
import jline.CursorBuffer;
import jline.FileNameCompletor;
import jline.ArgumentCompletor.WhitespaceArgumentDelimiter;

import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.CommandLineService;
import org.nuxeo.ecm.shell.CommandOption;
import org.nuxeo.ecm.shell.CommandParameter;
import org.nuxeo.ecm.shell.Token;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeCompletor implements Completor {

    private final InteractiveCommand cmd;
    private final CommandCompletor completor;
    private final CommandLineService service;
    private final ConsoleReader console;

    final Map<String, Completor> completors = new HashMap<String, Completor>();


    public CompositeCompletor(InteractiveCommand cmd) {
        this.cmd = cmd;
        service = cmd.getService();
        console = cmd.getConsole();
        completor = new CommandCompletor(service);
        completors.put("cmd", completor);
        completors.put("file", new FileNameCompletor());
        //completors.put("class", new ClassNameCompletor());
        completors.put("doc", new DocumentNameCompletor(service));
    }

    public void setCompletor(String name, Completor completor) {
        completors.put(name, completor);
    }

    public void removeCompletor(String name) {
        completors.remove(name);
    }

    public int complete(String buffer, int cursor, List candidates) {
        CursorBuffer buf = console.getCursorBuffer();
        jline.ArgumentCompletor.ArgumentList list = new WhitespaceArgumentDelimiter().delimit(
                buffer, cursor);
        String[] args = list.getArguments();
        String argText = list.getCursorArgument();
        int argIndex = list.getCursorArgumentIndex();
        int offset = list.getArgumentPosition();
//        ArgumentList list =  new ArgumentList(buf.toString(), cursor);
//        String[] args = list.args;
//        int argIndex = list.argIndex;
//        int offset = list.offset;
//        String argText = list.getArg();

        String text = argText == null ? null : argText.substring(0, offset);
        if (argIndex == 0) {
            int ret = completor.complete(text, offset, candidates);
            return ret + (list.getBufferPosition() - offset);
        } else {
            CommandDescriptor cd = service.getCommand(args[0]);
            if (cd == null) { // no such command
                return -1;
            }
            // now look into the command metadata to see how completion should be handled
            //int tokIndex = list.getBufferPosition();
            // parse cmd line
            if (buffer.endsWith(" ")) {
                String[] newArgs = new String[args.length+1];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[args.length] = "";
                args = newArgs;
            }
            CommandLine cmdLine;
            try {
                cmdLine = service.parse(args, false);
            } catch (ParseException e) {
                return -1;
            }

            if (argIndex >= cmdLine.size()) {
                return -1; // it happens for example when trying to complete "script -"
            }
            Token token = cmdLine.get(argIndex);
            if (token == null) {
                return -1;
            }
            if (token.type == Token.OPTION) {
                return -1; // no completion is made on options
            } else if (token.type == Token.VALUE) {
                if (!cd.hasOptions()) {
                    return -1;
                }
                // get the target option
                Token optToken = cmdLine.get(token.info);
                CommandOption opt = service.getCommandOption(optToken.value);
                if (opt == null) {
                    return -1;
                }
                String type = opt.getType();
                if (type == null || type.length() == 0) {
                    return -1;
                }
                Completor comp = completors.get(type);
                if (comp != null) {
                    int ret =  comp.complete(text, offset, candidates);
                    return ret + (list.getBufferPosition() - offset);
                }
            } else if (token.type == Token.PARAM) {
                if (!cd.hasArguments()) {
                    return -1;
                }
                for (CommandParameter param : cd.getArguments()) {
                    if (token.info == param.index) {
                        if (param.type == null || param.type.length() == 0) {
                            return -1;
                        }
                        Completor comp = completors.get(param.type);
                        if (comp != null) {
                            int ret = comp.complete(text, offset, candidates);
                            return ret + (list.getBufferPosition() - offset);
                        }
                    }
                }
            }

            return -1;
        }
    }

}
