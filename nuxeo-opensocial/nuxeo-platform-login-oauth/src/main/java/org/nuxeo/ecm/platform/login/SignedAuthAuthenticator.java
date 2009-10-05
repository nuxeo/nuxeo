/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.platform.login;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthMessage;
import net.oauth.server.OAuthServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.oauth.api.OAuthService;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.api.Framework;


public class SignedAuthAuthenticator implements NuxeoAuthenticationPlugin {


  protected static final String XOAUTH_SIGNATURE_PUBLIC_KEY = "xoauth_signature_publickey";
  private static final String OPENSOCIAL_VIEWER_ID = "opensocial_viewer_id";


  private static OAuthService service = null;


  private static final Log log = LogFactory.getLog(SignedAuthAuthenticator.class);




  public List<String> getUnAuthenticatedURLPrefix() {
    return null;
  }

  public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
      HttpServletResponse httpResponse, String baseURL) {
    return false;
  }

  public UserIdentificationInfo handleRetrieveIdentity(
      HttpServletRequest req, HttpServletResponse resp) {

    return verifyFetch(req, resp);

  }

  private UserIdentificationInfo verifyFetch(HttpServletRequest request,
      HttpServletResponse resp) {

    if(request.getParameter(OPENSOCIAL_VIEWER_ID) == null) {
      return null;
    }

    String consumerKey = request.getParameter(XOAUTH_SIGNATURE_PUBLIC_KEY);
    OAuthMessage message = OAuthServlet.getMessage(request, null);


    try {
      if(getOAuthService().verify(message, consumerKey)) {
        return getUserIdenticationInfoFromMessage(message);
      } else {
        log.info("User has not been recognized : returning null ");
        return null;
      }
    } catch (Exception e) {
      return null;
    }

  }

  private OAuthService getOAuthService() throws Exception {
    if(service == null) {
      service = Framework.getService(OAuthService.class);
    }
    return service;
  }

  private UserIdentificationInfo getUserIdenticationInfoFromMessage(
      OAuthMessage message) {
    try {
      String userId = message.getParameter(OPENSOCIAL_VIEWER_ID);
      log.info("User has been recognized :" + userId);
      return new UserIdentificationInfo(userId, userId);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return null;

  }


  public void initPlugin(Map<String, String> parameters) {
    //Do nothing
  }

  public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
    return false;
  }

}
