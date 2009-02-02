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

package org.nuxeo.ecm.webengine.client.console;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.Completor;
import jline.CursorBuffer;
import jline.FileNameCompletor;
import jline.ArgumentCompletor.WhitespaceArgumentDelimiter;

import org.nuxeo.ecm.webengine.client.command.Command;
import org.nuxeo.ecm.webengine.client.command.CommandRegistry;
import org.nuxeo.ecm.webengine.client.command.CommandToken;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeCompletor implements Completor {

    private final CommandRegistry registry;
    private final CommandCompletor completor;
    private final JLineConsole console;
    private final Map<String, Completor> completors = new HashMap<String, Completor>();
    private final Map<String,Completor> paramCompletors = new HashMap<String, Completor>();

    public CompositeCompletor(JLineConsole console, CommandRegistry registry) throws IOException {
        this.registry = registry;
        this.console = console;
        completor = new CommandCompletor(registry);
        completors.put("command", completor);
        completors.put("file", new FileNameCompletor());
        completors.put("dir", new DirectoryCompletor());
        completors.put("item", new DocumentNameCompletor(console));
        
        //classname completor is parsing system jars at startup. also it does't work on mac 
        // I get a java.util.zip.ZipException: error in opening zip file because it tries to load a lib file as a jar
        // disable it for now
        //completors.put("class", new ClassNameCompletor());
    }

    public void setCompletor(String name, Completor completor) {
        completors.put(name, completor);
    }

    public void removeCompletor(String name) {
        completors.remove(name);
    }

    public int complete(String buffer, int cursor, List candidates) {
        CursorBuffer buf = console.getReader().getCursorBuffer();
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

        // the prefix of the current arg (the chars on the left of the cursor)
        String argPrefix = argText == null ? null : argText.substring(0, offset);
        if (argIndex == 0) {
            int ret = completor.complete(argPrefix, offset, candidates);
            return ret + (list.getBufferPosition() - offset);
        } else {
//            System.out.println();
//            System.out.println("TODO completion");
//            System.out.println("# argtext: "+argText);
//            System.out.println("# args: "+Arrays.asList(args));
//            System.out.println("# argIndex: "+argIndex);
//            System.out.println("# offset: "+offset);
//            System.out.println("# argPrefix: "+argPrefix);

            Command cmd = registry.getCommand(args[0]);
            if (cmd == null) {
                return -1; // no such command
            }
            Completor comp = null;
            // get previous token and test if it requires a value
            if (argIndex > 1) { // if argIndex is 1 the previous is the command token if 0 then there is no previous
                CommandToken token = cmd.getSyntax().getToken(args[argIndex-1]);
                if (token != null) {
                    if (token.isValueRequired()) {
                        if (token.valueType != null) {
                            comp = completors.get(token.valueType);
                        } else {
                            return -1; // no completion available
                        }
                    }
                }
            }
            // the previous token has no completion available.
            // test for argument and option completion
            // 1. if command has arguments find out the next argument index and complete if argument support completion
            int k = 0;
            for (int i=1; i<argIndex; i++) {
                CommandToken token = cmd.getSyntax().getToken(args[i]);
                if (token != null && !token.isArgument()) { // skip options with values
                    if (token.isValueRequired()) {
                        i++; // skip value too
                    }
                    continue;
                }
                k++;
            }
            CommandToken token = cmd.getSyntax().getArgument(k);
            if (token != null && token.getValueType() != null) {
                comp = completors.get(token.getValueType());
            }
            // 2. if completor not found try to complete parameter names
            if (comp == null) {
                comp = paramCompletors.get(cmd.getName());
                if (comp == null) { // build param completor
                    comp = new ParameterNameCompletor(cmd);
                    // TODO we really need to cache this?
                    paramCompletors.put(cmd.getName(), comp);
                }
            }
            // complete if completor found
            if (comp != null) {
                int ret =  comp.complete(argPrefix, offset, candidates);
                return ret + (list.getBufferPosition() - offset);
            }
            return -1;
        }
    }


}
