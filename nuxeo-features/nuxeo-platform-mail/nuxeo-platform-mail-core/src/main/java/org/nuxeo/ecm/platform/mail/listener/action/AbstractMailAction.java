/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.listener.action;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.mail.action.ExecutionContext;
import org.nuxeo.ecm.platform.mail.action.MessageAction;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.CORE_SESSION_ID_KEY;

/**
 * @author Catalin Baican
 */
public abstract class AbstractMailAction implements MessageAction {

    protected CoreSession getCoreSession(ExecutionContext context)
            throws Exception {
        ExecutionContext initialContext = context.getInitialContext();
        String sessionId = (String) initialContext.get(CORE_SESSION_ID_KEY);
        if (sessionId != null) {
            return CoreInstance.getInstance().getSession(sessionId);
        }
        return null;
    }

}
