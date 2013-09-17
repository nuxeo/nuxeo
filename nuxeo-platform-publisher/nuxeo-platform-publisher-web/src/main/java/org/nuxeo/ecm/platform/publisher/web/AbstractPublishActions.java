/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 *     Marwane Kalam-Alami
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.ClientException;
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

    public String getFormattedPath(DocumentModel documentModel)
            throws ClientException {
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

    protected void getPathFragments(DocumentModel documentModel,
            List<String> pathFragments) throws ClientException {
        String pathElementName = documentModel.getTitle();
        String translatedPathElement = messages.get(pathElementName);
        pathFragments.add(translatedPathElement);

        if (isDomain(documentModel)
                || "/".equals(documentModel.getPathAsString())) {
            return;
        }

        DocumentModel parentDocument = getParentDocument(documentModel);
        if (parentDocument != null) {
            getPathFragments(parentDocument, pathFragments);
        }
    }

    protected DocumentModel getParentDocument(DocumentModel documentModel)
            throws ClientException {
        if (documentManager.hasPermission(documentModel.getParentRef(),
                SecurityConstants.READ)) {
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
