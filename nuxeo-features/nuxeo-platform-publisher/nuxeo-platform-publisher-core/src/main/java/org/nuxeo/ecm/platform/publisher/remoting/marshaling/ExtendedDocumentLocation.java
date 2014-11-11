/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

/**
 * Extension to {@link DocumentLocation} to provide information about source
 * server.
 *
 * @author tiry
 */
public class ExtendedDocumentLocation extends DocumentLocationImpl {

    private static final long serialVersionUID = 1L;

    protected String originalServer;

    public ExtendedDocumentLocation(String serverName, DocumentRef docRef) {
        super(serverName, docRef);
    }

    public ExtendedDocumentLocation(String originalServer, String serverName,
            DocumentRef docRef) {
        super(serverName, docRef);
        this.originalServer = originalServer;
    }

    public ExtendedDocumentLocation(String originalServer, DocumentModel doc) {
        super(doc);
        this.originalServer = originalServer;
    }

    public String getOriginalServer() {
        return this.originalServer;
    }

    @Override
    public String toString() {
        return getServerName() + "@" + getOriginalServer() + ":"
                + getDocRef().toString();
    }

    public static ExtendedDocumentLocation parseString(String source) {
        String[] refParts = source.split("@");
        String sourceServer = refParts[1].split(":")[0];
        String repositoryName = refParts[0];
        DocumentRef ref = new IdRef(refParts[1].split(":")[1]);
        return new ExtendedDocumentLocation(sourceServer, repositoryName, ref);
    }

    public static ExtendedDocumentLocation extractFromDoc(DocumentModel doc)
            throws ClientException {
        if (doc.hasSchema("dublincore")) {
            String source = (String) doc.getProperty("dublincore", "source");

            if (source != null) {
                return parseString(source);
            }
        }
        return null;
    }

}
