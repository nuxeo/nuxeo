/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.api.Framework;

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

    protected RenditionFinder(DocumentModel source, String renditionName) {
        super(source.getCoreSession());
        this.source = source;
        this.renditionName = renditionName;
    }

    @Override
    public void run() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ");
        query.append(RENDITION_NAME_PROPERTY);
        query.append(" = '");
        query.append(renditionName);
        query.append("' AND ");
        boolean isVersionable = source.isVersionable();
        String renditionSourceId = source.getId();
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
            query.append("ecm:isCheckedInVersion = 1 AND ");
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
        RenditionService rs = Framework.getService(RenditionService.class);
        RenditionDefinition def = ((RenditionServiceImpl) rs).getRenditionDefinition(renditionName);
        return def.getSourceDocumentModificationDatePropertyName();
    }

    protected static String formatTimestamp(Calendar cal) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(cal.getTime());
    }

}
