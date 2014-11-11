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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.action;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * @author Alexandre Russel
 */
public class Visitor {

    private final MessageActionPipe pipe;

    public Visitor(MessageActionPipe pipe) {
        this.pipe = pipe;
    }

    public void visit(Folder folder) throws Exception {
        visit(folder, null);
    }

    /**
     * Visit every message of given folder and every message of its subfolders.
     *
     * @param folder
     * @param initialContext context variables passed to each execution context
     */
    public void visit(Folder folder, ExecutionContext initialContext)
            throws Exception {
        for (Message message : folder.getMessages()) {
            ExecutionContext context = new ExecutionContext(message,
                    initialContext);
            for (MessageAction action : pipe) {
                action.reset(context);
                boolean result = action.execute(context);
                if (!result) {
                    break;
                }
            }
        }
        Folder[] folders = {};
        try {
            folders = folder.list();
        } catch (MessagingException e) {
            // do, nothing, list() not implemented.
        }
        for (Folder f : folders) {
            visit(f, initialContext);
        }
    }

    /**
     * Visit given messages
     *
     * @param messages
     * @param initialContext context variables passed to each execution context
     */
    public void visit(Message[] messages, ExecutionContext initialContext)
            throws Exception {
        for (Message message : messages) {
            ExecutionContext context = new ExecutionContext(message,
                    initialContext);
            for (MessageAction action : pipe) {
                action.reset(context);
                boolean result = action.execute(context);
                if (!result) {
                    break;
                }
            }
        }
    }

}
