/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RootSectionsManager {

    public static final String SCHEMA_PUBLISHING = "publishing";

    public static final String SECTIONS_PROPERTY_NAME = "publish:sections";

    protected static final String SECTION_ROOT_DOCUMENT_TYPE = "SectionRoot";

    protected CoreSession coreSession;

    public RootSectionsManager(CoreSession coreSession) {
        this.coreSession = coreSession;
    }

    public boolean canAddSection(DocumentModel section, DocumentModel currentDocument) {
        if (SECTION_ROOT_DOCUMENT_TYPE.equals(section.getType())) {
            return false;
        }
        String sectionId = section.getId();
        if (currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
            }

            if (sectionIdsList.contains(sectionId)) {
                return false;
            }
        }

        return true;
    }

    public String addSection(String sectionId, DocumentModel currentDocument) {

        if (sectionId != null && currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
                // make it resizable
                sectionIdsList = new ArrayList<>(sectionIdsList);
            }

            sectionIdsList.add(sectionId);
            String[] sectionIdsListIn = new String[sectionIdsList.size()];
            sectionIdsList.toArray(sectionIdsListIn);

            currentDocument.setPropertyValue(SECTIONS_PROPERTY_NAME, sectionIdsListIn);
            coreSession.saveDocument(currentDocument);
            coreSession.save();
        }
        return null;
    }

    public String removeSection(String sectionId, DocumentModel currentDocument) {

        if (sectionId != null && currentDocument.hasSchema(SCHEMA_PUBLISHING)) {
            String[] sectionIdsArray = (String[]) currentDocument.getPropertyValue(SECTIONS_PROPERTY_NAME);

            List<String> sectionIdsList = new ArrayList<>();

            if (sectionIdsArray != null && sectionIdsArray.length > 0) {
                sectionIdsList = Arrays.asList(sectionIdsArray);
                // make it resizable
                sectionIdsList = new ArrayList<>(sectionIdsList);
            }

            if (!sectionIdsList.isEmpty()) {
                sectionIdsList.remove(sectionId);

                String[] sectionIdsListIn = new String[sectionIdsList.size()];
                sectionIdsList.toArray(sectionIdsListIn);

                currentDocument.setPropertyValue(SECTIONS_PROPERTY_NAME, sectionIdsListIn);
                coreSession.saveDocument(currentDocument);
                coreSession.save();
            }
        }

        return null;
    }

}
