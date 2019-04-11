/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.template.adapters;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * Base class for shared code bewteen the {@link TemplateBasedDocument} and the {@link TemplateSourceDocument}.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public abstract class AbstractTemplateDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static Log log = LogFactory.getLog(AbstractTemplateDocument.class);

    protected DocumentModel adaptedDoc;

    protected CoreSession getSession() {
        if (adaptedDoc == null) {
            return null;
        }
        return adaptedDoc.getCoreSession();
    }

    public DocumentModel getAdaptedDoc() {
        return adaptedDoc;
    }

    protected void doSave() {
        adaptedDoc = getSession().saveDocument(adaptedDoc);
    }

    public DocumentModel save() {
        doSave();
        return adaptedDoc;
    }
}
