/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.onelogin.auth;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import com.onelogin.AccountSettings;
import com.onelogin.AppSettings;
import com.onelogin.saml.AuthRequest;
import com.onelogin.saml.Response;

public class OneLoginSAMLAuthenticationPlugin implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(OneLoginSAMLAuthenticationPlugin.class);

    protected static final String SAML_REQUEST = "SAMLRequest";

    protected static final String SAML_RESPONSE = "SAMLResponse";

    protected static final String RELAY_STATE = "RelayState";

    protected static final String OKTA_FILE_KEY = "OktaConfig";

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }      

    protected AccountSettings accSettings = new AccountSettings();
    
    protected AppSettings appSettings;
    
    protected synchronized AppSettings getAppSettings(HttpServletRequest request) {
        if (appSettings==null) {
            appSettings = new AppSettings();            
            String baseUrl = VirtualHostHelper.getBaseURL(request);            
            appSettings.setAssertionConsumerServiceUrl(baseUrl + "nxstartup.faces");
            appSettings.setIssuer(baseUrl);            
        }
        return appSettings;
    }
    
    public void initPlugin(Map<String, String> parameters) {
        try {
            accSettings = new AccountSettings();
            accSettings.setIdpSsoTargetUrl(parameters.get("url"));
            accSettings.setCertificate(parameters.get("cert"));            
        } catch (Exception e) {
            log.error("Error during Okta initialization", e);
        }
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
               
        AuthRequest authReq = new AuthRequest(getAppSettings(httpRequest), accSettings);
        
        String reqString= null;
        try {
            reqString = accSettings.getIdp_sso_target_url()+"?" + SAML_REQUEST + "=" + AuthRequest.getRidOfCRLF(URLEncoder.encode(authReq.getRequest(AuthRequest.base64),"UTF-8"));
        } catch (Exception e) {
           log.error("Error whil encoding URL", e);
           return false;
        }
        
       try {
            httpResponse.sendRedirect(reqString);
        } catch (IOException e) {
            String errorMessage = String.format(
                    "Unable to send redirect on %s", reqString);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        
        String userLogin = null;
        try {
            Response samlResponse = new Response(accSettings);
            samlResponse.loadXmlFromBase64(httpRequest.getParameter(SAML_RESPONSE));
            if (samlResponse.isValid()) {
                String userId = samlResponse.getNameId();
                if (userId == null || "".equals(userId)) {
                    return null;
                }            
                Map<String, List<String>> userAttributes = samlResponse.getAttributes();
                
                try {
                    userLogin = createOrUpdate(userId, null);
                } catch (Exception e) {
                    log.error("Unable to create or update user", e);
                }
            }
        } catch (Exception e) {
            log.error("Error while processing SAMLResponse", e);
            return null;
        }        
        
        if (userLogin!=null) {
            return new UserIdentificationInfo(userLogin, userLogin);    
        } else {
            return null;
        }        
    }

    protected String createOrUpdate(String userId,
            Map<String, Object> attributes) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);

        Session userDir = Framework.getService(DirectoryService.class).open(
                userManager.getUserDirectoryName());
        DocumentModel entry = null;

        try {
            entry = userDir.getEntry(userId);
            if (entry == null && userId.contains("@")) {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();                
                filter.put(userManager.getUserEmailField(), userId);
                DocumentModelList entries = userDir.query(filter);
                if (entries!=null && entries.size()>0) {
                    entry = entries.get(0);
                }
            }
            if (entry == null) {
                // userDir.createEntry(fieldMap);
            } else {
                if (attributes!=null && attributes.size()>0) {
                    entry.getDataModel(userManager.getUserSchemaName()).setMap(
                        attributes);                
                    userDir.updateEntry(entry);
                    userDir.commit();
                }
            }            
        } finally {
            userDir.close();
        }
        return (String) entry.getProperty(userManager.getUserSchemaName(), userManager.getUserIdField());
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

}
