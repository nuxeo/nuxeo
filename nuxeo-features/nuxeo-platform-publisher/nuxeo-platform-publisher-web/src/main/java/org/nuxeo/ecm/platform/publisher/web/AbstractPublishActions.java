/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractPublishActions {

    private static final Log log = LogFactory.getLog(AbstractPublishActions.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    public String getFormattedPath(DocumentModel documentModel)
            throws ClientException {
        List<String> pathFragments = new ArrayList<String>();
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
        String translatedPathElement = resourcesAccessor.getMessages().get(
                pathElementName);
        pathFragments.add(translatedPathElement);
        if ("Domain".equals(documentModel.getType())) {
            return;
        }

        DocumentModel parentDocument;
        if (documentManager.hasPermission(documentModel.getParentRef(),
                SecurityConstants.READ)) {
            parentDocument = documentManager.getDocument(documentModel.getParentRef());
            getPathFragments(parentDocument, pathFragments);
        }
    }

}
