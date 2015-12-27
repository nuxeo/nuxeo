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
