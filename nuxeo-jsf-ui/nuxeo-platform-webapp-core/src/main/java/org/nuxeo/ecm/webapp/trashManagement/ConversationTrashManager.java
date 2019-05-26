/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.trashManagement;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;

@Name("conversationTrashManager")
@Scope(APPLICATION)
public class ConversationTrashManager implements Serializable {

    private static final long serialVersionUID = 9876098763432L;

    private static final Log log = LogFactory.getLog(ConversationTrashManager.class);

    public void initTrashManager() {
        log.debug("Initialize");
    }

    public static boolean isTrashManagementEnabled() {
        return Framework.getService(TrashManagementService.class).isTrashManagementEnabled();
    }

    /**
     * @deprecated since 11.1. Use {@link Framework#getService(Class)} with {@link TrashManagementService} instead.
     */
    @Deprecated
    protected static TrashManagementService getService() {
        return Framework.getService(TrashManagementService.class);
    }

}
