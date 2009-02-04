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

    private static TrashManagementService trashService;

    public void initTrashManager() {
        log.debug("Initialize");
    }

    public static boolean isTrashManagementEnabled() {
        return getService().isTrashManagementEnabled();
    }

    protected static TrashManagementService getService() {
        if (trashService == null) {
            trashService = (TrashManagementService) Framework.getRuntime().getComponent(
                    TrashManagementService.NAME);
        }
        return trashService;
    }

}
