/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *     <a href="mailto:tmartins@nuxeo.com">Thierry Martins</a>
 */

package org.nuxeo.ecm.quota.size;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.quota.QuotaUtils;

/**
 * Simple factory for {@link QuotaAwareDocument} document model adapter
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class QuotaAwareDocumentFactory implements DocumentAdapterFactory {

    public static QuotaAwareDocument make(DocumentModel doc, boolean save) throws ClientException {
        if (!doc.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            doc.addFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET);
            if (save) {
                DocumentModel origDoc = doc;
                // set flags to disable listeners
                QuotaUtils.disableListeners(doc);
                doc = doc.getCoreSession().saveDocument(doc);
                // remove flags as they could be kept in the document for a long time otherwise
                QuotaUtils.clearContextData(doc);
                // also remove flags from original doc, which the caller still references
                QuotaUtils.clearContextData(origDoc);
            }
        }
        return (QuotaAwareDocument) doc.getAdapter(QuotaAware.class);
    }

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> adapter) {
        if (doc.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET)) {
            return adapter.cast(new QuotaAwareDocument(doc));
        }
        return null;
    }
}
