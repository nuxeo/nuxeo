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

import java.util.List;

import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * @author Alexandre Russel
 */
public class MailBoxActionsImpl implements MailBoxActions {

    protected final MessageActionPipe pipe = new MessageActionPipe();

    protected final Visitor visitor = new Visitor(pipe);

    protected final Folder folder;

    protected boolean expunge;

    public MailBoxActionsImpl(Folder folder, boolean expungeOnExit)
            throws MessagingException {
        this.folder = folder;
        folder.open(Folder.READ_ONLY);
        expunge = expungeOnExit;
    }

    public void addAction(MessageAction action) {
        pipe.add(action);
    }

    public void addActions(List<MessageAction> actions) {
        pipe.addAll(actions);
    }

    public void execute() throws Exception {
        execute(null);
    }

    public void execute(ExecutionContext initialContext) throws Exception {
        visitor.visit(folder, initialContext);
        folder.close(expunge);
    }

}
