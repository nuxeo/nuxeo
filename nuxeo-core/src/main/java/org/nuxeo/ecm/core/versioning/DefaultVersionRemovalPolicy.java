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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;

/**
 * Removes the version history if no proxies exist, otherwise do nothing.
 *
 * @author Florent Guillaume
 */
public class DefaultVersionRemovalPolicy implements VersionRemovalPolicy {

    private static final Log log = LogFactory.getLog(DefaultVersionRemovalPolicy.class);

    @Override
    public void removeVersions(Session session, Document doc,
            CoreSession coreSession) throws ClientException {
        try {
            if (session.getProxies(doc, null).isEmpty()) {
                List<String> versionsIds = doc.getVersionsIds();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Removing %s versions for: %s",
                            versionsIds.size(), doc.getUUID()));
                }
                for (String id : versionsIds) {
                    log.debug("Removing version: " + id);
                    coreSession.removeDocument(new IdRef(id));
                    // we don't use removeDocuments() as it needs paths
                }
            }
        } catch (DocumentException e) {
            throw new ClientException(e);
        }
    }
}
