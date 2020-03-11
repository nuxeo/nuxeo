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

    public MailBoxActionsImpl(Folder folder, boolean expungeOnExit) throws MessagingException {
        this.folder = folder;
        folder.open(Folder.READ_ONLY);
        expunge = expungeOnExit;
    }

    @Override
    public void addAction(MessageAction action) {
        pipe.add(action);
    }

    @Override
    public void addActions(List<MessageAction> actions) {
        pipe.addAll(actions);
    }

    @Override
    public void execute() throws MessagingException {
        execute(null);
    }

    @Override
    public void execute(ExecutionContext initialContext) throws MessagingException {
        visitor.visit(folder, initialContext);
        folder.close(expunge);
    }

}
