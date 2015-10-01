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

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * Retrives stored Rendition associated to a DocumentModel.
 * <p>
 * Can run Unrestricted or not.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class RenditionFinder extends UnrestrictedSessionRunner {

    protected final DocumentModel source;

    protected DocumentModel storedRendition;

    protected final String definitionName;

    protected RenditionFinder(DocumentModel source, String definitionName) {
        super(source.getCoreSession());
        this.source = source;
        this.definitionName = definitionName;
    }

    @Override
    public void run() {

        String query = "select * from Document where ecm:isProxy = 0 AND ";
        query = query + RENDITION_NAME_PROPERTY + "='" + definitionName + "' AND ";
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
            query = query + "ecm:isCheckedInVersion = 1 AND ";
        }
        query = query + RENDITION_SOURCE_ID_PROPERTY + "='" + renditionSourceId + "' ";
        query = query + "order by dc:modified desc ";

        List<DocumentModel> docs = session.query(query);
        if (docs.size() > 0) {
            storedRendition = docs.get(0);
            if (!isVersionable) {
                Calendar renditionLastModified = (Calendar) storedRendition.getPropertyValue("dc:modified");
                Calendar sourceLastModified = (Calendar) source.getPropertyValue("dc:modified");
                if (renditionLastModified == null || sourceLastModified == null
                        || sourceLastModified.after(renditionLastModified)) {
                    storedRendition = null;
                } else {
                    storedRendition.detach(true);
                }
            } else {
                storedRendition.detach(true);
            }
        }
    }

    public DocumentModel getStoredRendition() {
        return storedRendition;
    }

}
