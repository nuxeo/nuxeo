/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.finder;

import java.util.ArrayList;
import java.util.Collections;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;

/**
 * Helper class to manage:
 * <ul>
 * <li>unrestricted fetch of Sections
 * <li>filtering according to user rights
 * </ul>
 *
 * @author tiry
 */
public class DefaultRootSectionsFinder extends AbstractRootSectionsFinder
        implements RootSectionFinder {

    public DefaultRootSectionsFinder(CoreSession userSession) {
        super(userSession);
    }

    protected void computeUserSectionRoots(DocumentModel currentDoc)
            throws ClientException {
        this.currentDocument = currentDoc;
        this.runUnrestricted();

        if (currentDoc != null) {
            if (!unrestrictedSectionRootFromWorkspaceConfig.isEmpty()) {
                accessibleSectionRoots = getFiltredSectionRoots(
                        unrestrictedSectionRootFromWorkspaceConfig, true);
            } else {
                accessibleSectionRoots = getFiltredSectionRoots(
                        unrestrictedDefaultSectionRoot, true);
            }
        }
    }

    protected String buildQuery(String path) {
        // SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain'
        // and (ecm:primaryType = 'Section' or ecm:primaryType = 'SectionRoot'
        // )
        String query = "SELECT * FROM Document WHERE ecm:path STARTSWITH "
                + NXQL.escapeString(path) + " and (";

        int i = 0;
        for (String type : getSectionTypes()) {
            query = query + " ecm:primaryType = '" + type + "'";
            i++;
            if (i < getSectionTypes().size()) {
                query = query + " or ";
            } else {
                query = query + " )";
            }
        }
        query = query + " order by ecm:path ";
        return query;
    }

    protected void computeUnrestrictedRoots(CoreSession session)
            throws ClientException {

        if (currentDocument != null) {
            /*
             * Get the first parent having "publishing" schema. In order to void
             * infinite loop, if the parent is 'Root' type just break
             * (NXP-3359).
             */
            DocumentModel parentDocumentModel = currentDocument;
            while (!parentDocumentModel.hasSchema(SCHEMA_PUBLISHING)) {
                if ("Root".equals(parentDocumentModel.getType())) {
                    break;
                }
                parentDocumentModel = session.getDocument(parentDocumentModel.getParentRef());
            }

            DocumentModelList sectionRootsFromWorkspaceConfig = getSectionRootsFromWorkspaceConfig(
                    parentDocumentModel, session);
            unrestrictedSectionRootFromWorkspaceConfig = new ArrayList<String>();
            for (DocumentModel root : sectionRootsFromWorkspaceConfig) {
                unrestrictedSectionRootFromWorkspaceConfig.add(root.getPathAsString());
            }
        }

        if (unrestrictedDefaultSectionRoot == null) {
            unrestrictedDefaultSectionRoot = Collections.emptyList();
        }
    }

}
