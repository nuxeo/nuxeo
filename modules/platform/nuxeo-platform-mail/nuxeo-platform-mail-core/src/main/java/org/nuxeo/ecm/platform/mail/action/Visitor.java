/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public void visit(Folder folder) throws MessagingException {
        visit(folder, null);
    }

    /**
     * Visit every message of given folder and every message of its subfolders.
     *
     * @param folder
     * @param initialContext context variables passed to each execution context
     */
    public void visit(Folder folder, ExecutionContext initialContext) throws MessagingException {
        for (Message message : folder.getMessages()) {
            ExecutionContext context = new ExecutionContext(message, initialContext);
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
    public void visit(Message[] messages, ExecutionContext initialContext) throws MessagingException {
        for (Message message : messages) {
            ExecutionContext context = new ExecutionContext(message, initialContext);
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
