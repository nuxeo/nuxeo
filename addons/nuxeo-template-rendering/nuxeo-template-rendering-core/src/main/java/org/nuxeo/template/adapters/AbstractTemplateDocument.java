/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.adapters;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * Base class for shared code bewteen the {@link TemplateBasedDocument} and the
 * {@link TemplateSourceDocument}.
 * 
 * @author Tiry (tdelprat@nuxeo.com)
 * 
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

    protected void doSave() throws ClientException {
        adaptedDoc = getSession().saveDocument(adaptedDoc);
    }

    public DocumentModel save() throws ClientException {
        doSave();
        return adaptedDoc;
    }
}
