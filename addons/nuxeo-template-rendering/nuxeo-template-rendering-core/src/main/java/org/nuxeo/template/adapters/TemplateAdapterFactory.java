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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.template.adapters.doc.TemplateBasedDocumentAdapterImpl;
import org.nuxeo.template.adapters.source.TemplateSourceDocumentAdapterImpl;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

/**
 * Pluggable {@link DocumentAdapterFactory} used to return the right {@link TemplateBasedDocument} or
 * {@link TemplateSourceDocument} implementation according to given {@link DocumentModel}.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class TemplateAdapterFactory implements DocumentAdapterFactory {

    protected static final Log log = LogFactory.getLog(TemplateAdapterFactory.class);

    /**
     * Checks if the document can be adapted. Also works on a ShallowDocumentModel.
     */
    public static boolean isAdaptable(DocumentModel doc, Class<?> adapterClass) {
        if (adapterClass == TemplateBasedDocument.class) {
            return doc.hasFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
        }
        if (adapterClass == TemplateSourceDocument.class) {
            return doc.hasFacet(TemplateSourceDocumentAdapterImpl.TEMPLATE_FACET);
        }
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(DocumentModel doc, Class adapterClass) {

        if (adapterClass == TemplateBasedDocument.class) {
            if (doc.hasFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET)) {
                return new TemplateBasedDocumentAdapterImpl(doc);
            } else {
                return null;
            }
        }

        if (adapterClass == TemplateSourceDocument.class) {
            if (doc.hasFacet(TemplateSourceDocumentAdapterImpl.TEMPLATE_FACET)) {
                return new TemplateSourceDocumentAdapterImpl(doc);
            } else {
                return null;
            }
        }

        return null;
    }
}
