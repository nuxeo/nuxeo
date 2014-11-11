/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.versioning;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;

/**
 * Interface for the policy that decides which versions have to be removed when
 * a working document is removed. This policy is called at the AbstractSession
 * level.
 *
 * @author Florent Guillaume
 */
public interface VersionRemovalPolicy {

    /**
     * Removes the versions when a given working document is about to be
     * removed.
     *
     * @param session the current session
     * @param doc the document that is about to be removed
     */
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException;

}
