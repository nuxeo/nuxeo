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

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import org.nuxeo.ecm.platform.mail.action.ExecutionContext;

/**
 * @author Catalin Baican
 */
public class StartAction extends AbstractMailAction {

    @Override
    public boolean execute(ExecutionContext context) throws MessagingException {
        Message message = context.getMessage();
        if (message == null) {
            return false;
        }
        Flags flags = message.getFlags();
        if (flags != null && flags.contains(Flag.SEEN)) {
            return false;
        }
        return true;
    }

}
