/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.web.common.MobileBannerHelper;

/**
 * Actions for the banner to open a document in the mobile application.
 *
 * @since 9.1
 */
@Name("mobileBannerActions")
@Scope(CONVERSATION)
public class MobileBannerActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    public String getURLForAndroidApplication() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                                                                      .getExternalContext()
                                                                      .getRequest();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return MobileBannerHelper.getURLForAndroidApplication(request, currentDocument);
    }

    public String getURLForIOSApplication() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
                                                                      .getExternalContext()
                                                                      .getRequest();
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return MobileBannerHelper.getURLForIOSApplication(request, currentDocument);
    }

    @Factory(value = "appStoreURL", scope = APPLICATION)
    public String getAppStoreURL() {
        return MobileBannerHelper.getAppStoreURL();
    }

}
