/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.userdata;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class UserDataAdaptorImpl implements UserDataAdaptor {

    final DocumentModel domain;
    final UserDataManager userDataManager;
    final String domainPath;
    final String sessionId;

    UserDataAdaptorImpl(DocumentModel domain) {
        this.domain = domain;
        domainPath = domain.getPathAsString();
        sessionId = domain.getSessionId();
        userDataManager = UserDataServiceHelper.getService().getManager();
    }

    private CoreSession getSession() {
        return CoreInstance.getInstance().getSession(sessionId);
    }

    public void add(String username, String category, DocumentModel docModel)
            throws ClientException {
        userDataManager.add(domainPath, getSession(), username, category, docModel);
    }

    public DocumentModelList get(String username, String category) throws ClientException {
        return userDataManager.get(domainPath, getSession(), username, category);
    }

    public void remove(String username, String category, DocumentModel docModel)
            throws ClientException {
        userDataManager.remove(domainPath, getSession(), username, category, docModel);
    }

}
