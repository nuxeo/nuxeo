/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_VARIANT_PROPERTY;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Retrieves stored Rendition associated to a DocumentModel.
 * <p>
 * Can run Unrestricted or not.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class RenditionFinder extends UnrestrictedSessionRunner {

    protected final DocumentModel source;

    protected DocumentModel storedRendition;

    protected final String renditionName;

    /**
     * @since 8.1
     */
    protected RenditionDefinition renditionDefinition;

    /**
     * @since 8.1
     */
    protected final String renditionVariant;

    /**
     * @since 8.1
     */
    protected RenditionFinder(DocumentModel source, RenditionDefinition renditionDefinition) {
        super(source.getCoreSession());
        this.source = source;
        this.renditionDefinition = renditionDefinition;
        renditionName = renditionDefinition.getName();
        renditionVariant = renditionDefinition.getProvider().getVariant(source, renditionDefinition);
    }

    @Override
    public void run() {
        boolean isVersionable = source.isVersionable();
        String renditionSourceId = source.getId();
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ");
        query.append(RENDITION_NAME_PROPERTY);
        query.append(" = '");
        query.append(NXQL.escapeStringInner(renditionName));
        query.append("' AND ");
        if (renditionVariant != null) {
            query.append(RENDITION_VARIANT_PROPERTY);
            query.append(" = '");
            query.append(NXQL.escapeStringInner(renditionVariant));
            query.append("' AND ");
        }
        if (isVersionable) {
            if (!source.isVersion() && !source.isCheckedOut()) {
                DocumentModel lastVersion = session.getLastDocumentVersion(source.getRef());
                if (lastVersion != null) {
                    renditionSourceId = lastVersion.getId();
                } else {
                    // no version at all
                    return;
                }
            }
            query.append("ecm:isVersion = 1 AND ");
        } else {
            String modificationDatePropertyName = getSourceDocumentModificationDatePropertyName();
            Calendar sourceLastModified = (Calendar) source.getPropertyValue(modificationDatePropertyName);
            if (sourceLastModified != null) {
                query.append(RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY);
                query.append(" >= ");
                query.append(formatTimestamp(sourceLastModified));
                query.append(" AND ");
            }
        }
        query.append(RENDITION_SOURCE_ID_PROPERTY);
        query.append(" = '");
        query.append(renditionSourceId);
        query.append("' ORDER BY dc:modified DESC");

        List<DocumentModel> docs = session.query(query.toString());
        if (docs.size() > 0) {
            storedRendition = docs.get(0);
            storedRendition.detach(true);
        }
    }

    public DocumentModel getStoredRendition() {
        return storedRendition;
    }

    protected String getSourceDocumentModificationDatePropertyName() {
        return renditionDefinition.getSourceDocumentModificationDatePropertyName();
    }

    protected static String formatTimestamp(Calendar cal) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(cal.getTime());
    }

}
