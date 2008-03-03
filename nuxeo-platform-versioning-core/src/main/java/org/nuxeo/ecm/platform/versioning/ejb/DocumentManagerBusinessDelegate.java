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
 * $Id$
 */

package org.nuxeo.ecm.platform.versioning.ejb;

import java.io.Serializable;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Document manager business delegate.
 *
 */
public class DocumentManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentManagerBusinessDelegate.class);

    protected transient CoreSession documentManager;

    public CoreSession getDocumentManager(String repositoryUri,
            Map<String, Serializable> sessionContext) throws NamingException,
            ClientException {
        log.info("<getDocumentManager>");

        // first destroy if needed
        if (null != documentManager) {
            log.info("Removing the documentManager first.");
            remove();
        }

        documentManager = CoreInstance.getInstance().open(
                repositoryUri, sessionContext);

        log.info("DocumentManager bean found :"
                + documentManager.getClass().toString());
        return documentManager;
    }

    public void remove() throws ClientException {
        // TODO: removing the session produces a failure on the next new session
        // open - need to investigate why
        //CoreInstance.getInstance().close(documentManager);

        if (null != documentManager) {
            documentManager.destroy();
            documentManager = null;
        }
    }

}
