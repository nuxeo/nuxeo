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
 *     Marwane Kalam-Alami
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractPublishActions {

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected Map<String, String> messages;

    public String getFormattedPath(DocumentModel documentModel) {
        List<String> pathFragments = new ArrayList<>();
        getPathFragments(documentModel, pathFragments);
        return formatPathFragments(pathFragments);
    }

    protected static String formatPathFragments(List<String> pathFragments) {
        String fullPath = "";
        for (String aFragment : pathFragments) {
            if (!"".equals(fullPath)) {
                fullPath = ">" + fullPath;
            }
            fullPath = aFragment + fullPath;
        }
        return fullPath;
    }

    protected void getPathFragments(DocumentModel documentModel, List<String> pathFragments) {
        String pathElementName = documentModel.getTitle();
        String translatedPathElement = messages.get(pathElementName);
        pathFragments.add(translatedPathElement);

        if (isDomain(documentModel) || "/".equals(documentModel.getPathAsString())) {
            return;
        }

        DocumentModel parentDocument = getParentDocument(documentModel);
        if (parentDocument != null) {
            getPathFragments(parentDocument, pathFragments);
        }
    }

    protected DocumentModel getParentDocument(DocumentModel documentModel) {
        if (documentManager.hasPermission(documentModel.getParentRef(), SecurityConstants.READ)) {
            return documentManager.getDocument(documentModel.getParentRef());
        }
        return null;
    }

    protected boolean isDomain(DocumentModel documentModel) {
        Type type = documentModel.getDocumentType();
        while (type != null) {
            if ("Domain".equals(type.getName())) {
                return true;
            }
            type = type.getSuperType();
        }
        return false;
    }

}
