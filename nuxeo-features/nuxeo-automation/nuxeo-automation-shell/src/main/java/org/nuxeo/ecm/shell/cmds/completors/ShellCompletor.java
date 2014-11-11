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

package org.nuxeo.ecm.shell.cmds.completors;

import java.util.List;

import jline.ArgumentCompletor.WhitespaceArgumentDelimiter;
import jline.Completor;

import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.Interactive;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ShellCompletor implements Completor {

    private final Interactive interactive;

    private final CommandCompletor completor;

    public ShellCompletor(Interactive interactive) {
        this.interactive = interactive;
        completor = new CommandCompletor(interactive.getShell());
    }

    @SuppressWarnings("rawtypes")
    public int complete(String buffer, int cursor, List candidates) {
        jline.ArgumentCompletor.ArgumentList list = new WhitespaceArgumentDelimiter().delimit(
                buffer, cursor);
        String[] args = list.getArguments();
        // the current arg text (null if empty - i.e. after a space)
        String argText = list.getCursorArgument();
        // the offset of the current arg
        int argIndex = list.getCursorArgumentIndex();
        // the offset of the next character relative to the cursor in the
        // current arg
        int offset = list.getArgumentPosition();
        // the leading text in the current arg
        String text = argText == null ? null : argText.substring(0, offset);
        if (argIndex == 0) {
            int ret = completor.complete(text, offset, candidates);
            return ret + (list.getBufferPosition() - offset);
        } else {
            Shell shell = interactive.getShell();
            CommandType cmd = shell.getActiveRegistry().getCommandType(args[0]);
            if (cmd == null) { // no such command
                return -1;
            }
            if (argIndex >= args.length) {
                // at the beginning of a new token
                String[] newArgs = new String[args.length + 1];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                newArgs[args.length] = "";
                args = newArgs;
            } else if (argIndex < args.length) { // TODO
                String[] newArgs = new String[argIndex + 1];
                System.arraycopy(args, 0, newArgs, 0, newArgs.length);
                args = newArgs;
            }
            Completor comp = cmd.getLastTokenCompletor(shell, args);
            if (comp != null) {
                int ret = comp.complete(text, offset, candidates);
                return ret + (list.getBufferPosition() - offset);
            }
            return -1;
        }
    }

}
