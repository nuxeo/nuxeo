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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;

/**
 * This class knows how to resolve a Document from a document reference. It is
 * intended to be used inside this module ({@link org.nuxeo.ecm.core.api}) and
 * not exposed to the clients.
 * <p>
 * It is factored out from {@link AbstractSession} because other classes need to
 * use this functionality (security classes for example) and we don't want this
 * functionality exposed to the clients.
 *
 * @author Razvan Caraghin
 */
// XXX: this is actually untrue: only AbstractSession references this class. This
// could be refactored.
public class DocumentResolver implements Serializable {

    private static final long serialVersionUID = -2261223293670404568L;

    private static final Log log = LogFactory.getLog(DocumentResolver.class);

    /**
     * Resolves the document given its reference.
     *
     * @param session
     * @param docRef
     * @return
     * @throws DocumentException if the document could not be resolved
     */
    public static Document resolveReference(Session session, DocumentRef docRef)
            throws DocumentException {
        if (docRef == null) {
            throw new DocumentException("Invalid reference (null)");
        }
        int type = docRef.type();
        Object ref = docRef.reference();
        if (ref == null) {
            throw new DocumentException("Invalid reference (null)");
        }
        switch (type) {
            case DocumentRef.ID:
                return session.getDocumentByUUID((String) ref);
            case DocumentRef.PATH:
                return session.resolvePath((String) ref);
            case DocumentRef.INSTANCE:
                return (Document) ref;
            default:
                log.error("Invalid document reference type: " + type);
                return null;
        }
    }

}
