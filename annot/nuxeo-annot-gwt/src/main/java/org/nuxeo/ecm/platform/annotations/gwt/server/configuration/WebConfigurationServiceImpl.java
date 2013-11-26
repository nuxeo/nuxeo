/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
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
 *
 */
public class WebConfigurationServiceImpl extends RemoteServiceServlet implements
        WebConfigurationService {

    private static final long serialVersionUID = 2389527283775608787L;

    private static final Log log = LogFactory.getLog(WebConfigurationServiceImpl.class);

    private static WebAnnotationConfigurationService webAnnotationConfigurationService;

    protected DocumentViewCodecManager documentViewCodecManager;

    private NuxeoPrincipal currentUser;

    protected static WebAnnotationConfigurationService getConfig() {
        if (webAnnotationConfigurationService==null) {
            try {
                webAnnotationConfigurationService = Framework.getService(WebAnnotationConfigurationService.class);
            } catch (Exception e) {
                log.error(
                        "Unable to find WebAnnotationConfigurationService service",
                        e);
            }
        }
        return webAnnotationConfigurationService;
    }


    public WebConfiguration getWebConfiguration(String url) {
        WebConfiguration conf = new WebConfiguration();

        List<WebAnnotationDefinitionDescriptor> types = getConfig().getEnabledWebAnnotationDefinitions();

        for (WebAnnotationDefinitionDescriptor type : types) {
            Map<String, String[]> fields = new HashMap<String, String[]>();
            for (WebAnnotationFieldDescriptor field : type.getFields()) {
                fields.put(field.getName(), field.getChoices());
            }

            conf.addAnnotationDefinition(new AnnotationDefinition(
                    type.getUri(), type.getName(), type.getIcon(),
                    type.getType(), type.getListIcon(), type.getCreateIcon(),
                    type.isInMenu(), fields));
        }

        UserInfoMapper userInfoMapper = getConfig().getUserInfoMapper();
        if (userInfoMapper != null) {
            conf.setUserInfo(userInfoMapper.getUserInfo(currentUser));
        }

        WebPermission webPermission = getConfig().getWebPermission();
        if (webPermission != null) {
            conf.setCanAnnotate(canAnnotate(url, webPermission));
        }

        Map<String, FilterDescriptor> filters = getConfig().getFilterDefinitions();
        for (FilterDescriptor filter : filters.values()) {
            conf.addFilter(filter.getOrder(), filter.getName(),
                    filter.getIcon(), filter.getType(), filter.getAuthor(),
                    filter.getFields());
        }

        conf.setDisplayedFields(getConfig().getDisplayedFields());
        conf.setFieldLabels(getConfig().getFieldLabels());
        return conf;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        currentUser = (NuxeoPrincipal) request.getUserPrincipal();
        super.service(request, response);
    }

    protected boolean canAnnotate(String url, WebPermission webPermission) {
        DocumentViewCodecManager documentViewCodecManager = getDocumentViewCodecManager();
        DocumentView docView = documentViewCodecManager.getDocumentViewFromUrl(
                url, true, getBaseUrl(url));
        DocumentLocation docLocation = docView.getDocumentLocation();

        CoreSession coreSession = null;
        try {
            coreSession = getCoreSession(docLocation);
            DocumentModel docModel = coreSession.getDocument(docLocation.getDocRef());
            return webPermission.canAnnotate(docModel);
        } catch (ClientException e) {
            log.error("Unable to get Document: " + docLocation.getDocRef(), e);
        } finally {
            if (coreSession != null) {
                CoreInstance.getInstance().close(coreSession);
            }
        }

        return true; // if any error, default to authorize annotations
    }

    protected DocumentViewCodecManager getDocumentViewCodecManager() {
        if (documentViewCodecManager == null) {
            try {
                documentViewCodecManager = Framework.getService(DocumentViewCodecManager.class);
            } catch (Exception e) {
                log.error("Unable to get DocumentViewCodecManager", e);
            }
        }
        return documentViewCodecManager;
    }

    protected String getBaseUrl(String url) {
        String nxUrl = VirtualHostHelper.getContextPathProperty() + "/";
        return url.substring(0, url.lastIndexOf(nxUrl) + nxUrl.length());
    }

    protected CoreSession getCoreSession(DocumentLocation docLocation)
            throws ClientException {
        CoreSession session = CoreInstance.getInstance().open(
                docLocation.getServerName(), null);
        return session;
    }

}
