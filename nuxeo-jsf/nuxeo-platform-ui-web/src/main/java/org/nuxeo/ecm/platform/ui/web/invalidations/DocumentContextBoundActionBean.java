/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.invalidations;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base class for Seam beans that use the Automatic invalidation system
 *
 * @author tiry
 */
public abstract class DocumentContextBoundActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private DocumentModel currentDocument;

    protected DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    @DocumentContextInvalidation
    public void onContextChange(DocumentModel doc) {
        if (doc == null) {
            currentDocument = null;
            resetBeanCache(null);
            return;
        } else if (currentDocument == null) {
            currentDocument = doc;
            resetBeanCache(doc);
            return;
        }
        if (!doc.getRef().equals(currentDocument.getRef())) {
            currentDocument = doc;
            resetBeanCache(doc);
        }
    }

    protected abstract void resetBeanCache(DocumentModel newCurrentDocumentModel);

}
