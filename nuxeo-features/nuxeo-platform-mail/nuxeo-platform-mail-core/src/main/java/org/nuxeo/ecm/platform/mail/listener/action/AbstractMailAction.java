/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CORE_SESSION_KEY;

import javax.mail.MessagingException;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageAction;

/**
 * @author Catalin Baican
 * @author Laurent Doguin
 */
public abstract class AbstractMailAction implements MessageAction {

    @Override
    public boolean execute(ExecutionContext context) throws MessagingException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void reset(ExecutionContext context) {
        // do nothing
    }

    protected CoreSession getCoreSession(ExecutionContext context) {
        ExecutionContext initialContext = context.getInitialContext();
        return (CoreSession) initialContext.get(CORE_SESSION_KEY);
    }

}
