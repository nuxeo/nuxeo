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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

/**
 * Extension to {@link DocumentLocation} to provide information about source server.
 *
 * @author tiry
 */
public class ExtendedDocumentLocation extends DocumentLocationImpl {

    private static final long serialVersionUID = 1L;

    protected String originalServer;

    public ExtendedDocumentLocation(String serverName, DocumentRef docRef) {
        super(serverName, docRef);
    }

    public ExtendedDocumentLocation(String originalServer, String serverName, DocumentRef docRef) {
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
        return getServerName() + "@" + getOriginalServer() + ":" + getDocRef().toString();
    }

    public static ExtendedDocumentLocation parseString(String source) {
        String[] refParts = source.split("@");
        String sourceServer = refParts[1].split(":")[0];
        String repositoryName = refParts[0];
        DocumentRef ref = new IdRef(refParts[1].split(":")[1]);
        return new ExtendedDocumentLocation(sourceServer, repositoryName, ref);
    }

    public static ExtendedDocumentLocation extractFromDoc(DocumentModel doc) {
        if (doc.hasSchema("dublincore")) {
            String source = (String) doc.getProperty("dublincore", "source");

            if (source != null) {
                return parseString(source);
            }
        }
        return null;
    }

}
