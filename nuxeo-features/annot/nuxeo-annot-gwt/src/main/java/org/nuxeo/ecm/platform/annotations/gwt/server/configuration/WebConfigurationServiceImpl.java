/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.server.configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.configuration.service.FilterDescriptor;
import org.nuxeo.ecm.platform.annotations.configuration.service.WebAnnotationConfigurationService;
import org.nuxeo.ecm.platform.annotations.configuration.service.WebAnnotationDefinitionDescriptor;
import org.nuxeo.ecm.platform.annotations.configuration.service.WebAnnotationFieldDescriptor;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfigurationService;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class WebConfigurationServiceImpl extends RemoteServiceServlet implements WebConfigurationService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WebConfigurationServiceImpl.class);

    private NuxeoPrincipal currentUser;

    public WebConfiguration getWebConfiguration(String url) {
        WebAnnotationConfigurationService config = Framework.getService(WebAnnotationConfigurationService.class);
        WebConfiguration conf = new WebConfiguration();

        List<WebAnnotationDefinitionDescriptor> types = config.getEnabledWebAnnotationDefinitions();

        for (WebAnnotationDefinitionDescriptor type : types) {
            Map<String, String[]> fields = new HashMap<String, String[]>();
            for (WebAnnotationFieldDescriptor field : type.getFields()) {
                fields.put(field.getName(), field.getChoices());
            }

            conf.addAnnotationDefinition(new AnnotationDefinition(type.getUri(), type.getName(), type.getIcon(),
                    type.getType(), type.getListIcon(), type.getCreateIcon(), type.isInMenu(), fields));
        }

        UserInfoMapper userInfoMapper = config.getUserInfoMapper();
        if (userInfoMapper != null) {
            conf.setUserInfo(userInfoMapper.getUserInfo(currentUser));
        }

        WebPermission webPermission = config.getWebPermission();
        if (webPermission != null) {
            conf.setCanAnnotate(canAnnotate(url, webPermission));
        }

        Map<String, FilterDescriptor> filters = config.getFilterDefinitions();
        for (FilterDescriptor filter : filters.values()) {
            conf.addFilter(filter.getOrder(), filter.getName(), filter.getIcon(), filter.getType(), filter.getAuthor(),
                    filter.getFields());
        }

        conf.setDisplayedFields(config.getDisplayedFields());
        conf.setFieldLabels(config.getFieldLabels());
        return conf;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        currentUser = (NuxeoPrincipal) request.getUserPrincipal();
        super.service(request, response);
    }

    protected boolean canAnnotate(String url, WebPermission webPermission) {
        DocumentViewCodecManager documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        DocumentView docView = documentViewCodecManager.getDocumentViewFromUrl(url, true, getBaseUrl(url));
        DocumentLocation docLocation = docView.getDocumentLocation();
        try (CloseableCoreSession coreSession = CoreInstance.openCoreSession(docLocation.getServerName())) {
            DocumentModel docModel = coreSession.getDocument(docLocation.getDocRef());
            return webPermission.canAnnotate(docModel);
        } catch (DocumentNotFoundException e) {
            log.error("Unable to get Document: " + docLocation.getDocRef(), e);
        }
        return true; // if any error, default to authorize annotations
    }

    protected String getBaseUrl(String url) {
        String nxUrl = VirtualHostHelper.getContextPathProperty() + "/";
        return url.substring(0, url.lastIndexOf(nxUrl) + nxUrl.length());
    }

}
