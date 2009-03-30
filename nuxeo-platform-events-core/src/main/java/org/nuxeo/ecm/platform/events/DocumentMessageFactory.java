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
 *
 * $Id: DocumentMessageFactory.java 28510 2008-01-06 10:21:44Z sfermigier $
 */

package org.nuxeo.ecm.platform.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;

/**
 * Document message factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class DocumentMessageFactory {

    private static final Log log = LogFactory.getLog(DocumentMessageFactory.class);

    // Utility class.
    private DocumentMessageFactory() {
    }

    /**
     * @deprecacted not used - will be removed in 5.2
     */
    @Deprecated
    public static DocumentMessage createDocumentMessage(Document doc)
            throws DocumentException {
        DocumentMessage documentMessage;
        try {
            DocumentModel dm = DocumentModelFactory.createDocumentModel(doc);
            documentMessage = new DocumentMessageImpl(dm);
        } catch (NullPointerException npe) {
            log.error("Impossible to construct the document message....");
            documentMessage = new DocumentMessageImpl();
            // npe.printStackTrace();
        }
        return documentMessage;
    }

    public static DocumentMessage createDocumentMessage(DocumentModel model) {
        return new DocumentMessageImpl(model);
    }

    public static DocumentMessage createDocumentMessage(Document doc,
            CoreEvent coreEvent) throws DocumentException {
        DocumentMessage documentMessage;
        try {
            DocumentModel dm = DocumentModelFactory.createDocumentModel(doc);
            return createDocumentMessage(dm, coreEvent);
        } catch (NullPointerException npe) {
            log.error("Impossible to construct the document message....");
            documentMessage = new DocumentMessageImpl();
            // npe.printStackTrace();
        }
        return documentMessage;
    }

    public static DocumentMessage createDocumentMessage(DocumentModel dm,
            CoreEvent coreEvent) throws DocumentException {

        return new DocumentMessageImpl(dm, coreEvent);

    }

}
