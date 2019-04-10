/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.dam.webapp.annotations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.webapp.contentbrowser.DamDocumentActions;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.ui.web.rest.RestHelper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("annotationsActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class AnnotationsActions {

    private static final Log log = LogFactory.getLog(AnnotationsActions.class);

    @In(create = true)
    protected transient DamDocumentActions damDocumentActions;

    private URLPolicyService urlService;

    private URLPolicyService getUrlService() {
        if (urlService == null) {
            try {
                urlService = Framework.getService(URLPolicyService.class);
            } catch (Exception e) {
                log.error("Could not retrieve the URLPolicyService", e);
            }
        }
        return urlService;
    }

    /**
     * Method used to generate Annotation Url provided to facelet
     */
    public String getAnnotationsURL() {
        return getAnnotationsURL(damDocumentActions.getCurrentSelection());
    }

    public String getAnnotationsURL(DocumentModel document) {
        DocumentLocation docLocation = new DocumentLocationImpl(
                document.getRepositoryName(), document.getRef());
        DocumentView docView = new DocumentViewImpl(docLocation,
                "annotations_popup");
        String url = getUrlService().getUrlFromDocumentView(docView,
                BaseURL.getBaseURL());
        url = RestHelper.addCurrentConversationParameters(url);
        return url;
    }

}
