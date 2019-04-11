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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.impl.finder;

import java.util.ArrayList;
import java.util.Collections;

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
public class DefaultRootSectionsFinder extends AbstractRootSectionsFinder implements RootSectionFinder {

    public DefaultRootSectionsFinder(CoreSession userSession) {
        super(userSession);
    }

    @Override
    protected void computeUserSectionRoots(DocumentModel currentDoc) {
        this.currentDocument = currentDoc;
        this.runUnrestricted();

        if (currentDoc != null) {
            if (!unrestrictedSectionRootFromWorkspaceConfig.isEmpty()) {
                accessibleSectionRoots = getFiltredSectionRoots(unrestrictedSectionRootFromWorkspaceConfig, true);
            } else {
                accessibleSectionRoots = getFiltredSectionRoots(unrestrictedDefaultSectionRoot, true);
            }
        }
    }

    @Override
    protected String buildQuery(String path) {
        // SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain'
        // and (ecm:primaryType = 'Section' or ecm:primaryType = 'SectionRoot'
        // )
        String query = "SELECT * FROM Document WHERE ecm:path STARTSWITH " + NXQL.escapeString(path) + " and (";

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

    @Override
    protected void computeUnrestrictedRoots(CoreSession session) {

        if (currentDocument != null) {
            /*
             * Get the first parent having "publishing" schema. In order to void infinite loop, if the parent is 'Root'
             * type just break (NXP-3359).
             */
            DocumentModel parentDocumentModel = currentDocument;
            while (!parentDocumentModel.hasSchema(SCHEMA_PUBLISHING)) {
                if ("Root".equals(parentDocumentModel.getType())) {
                    break;
                }
                parentDocumentModel = session.getDocument(parentDocumentModel.getParentRef());
            }

            DocumentModelList sectionRootsFromWorkspaceConfig = getSectionRootsFromWorkspaceConfig(parentDocumentModel,
                    session);
            unrestrictedSectionRootFromWorkspaceConfig = new ArrayList<>();
            for (DocumentModel root : sectionRootsFromWorkspaceConfig) {
                unrestrictedSectionRootFromWorkspaceConfig.add(root.getPathAsString());
            }
        }

        if (unrestrictedDefaultSectionRoot == null) {
            unrestrictedDefaultSectionRoot = Collections.emptyList();
        }
    }

}
