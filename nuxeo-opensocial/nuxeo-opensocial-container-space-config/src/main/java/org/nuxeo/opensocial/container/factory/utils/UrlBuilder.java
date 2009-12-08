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

package org.nuxeo.opensocial.container.factory.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.utils.SecureTokenBuilder;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

/**
 * URlBuilder is builder of gadget url; Util for render of gadget into Shinding
 * opensocial server
 * 
 */
public class UrlBuilder {

  private static final String SERVLET_PATH = "/nuxeo/opensocial/gadgets/ifr";

  private static final String CONTAINER_KEY = "container";
  private static final String CONTAINER_VALUE = "default";

  private static final String GADGET_ID_KEY = "mid";

  private static final String NOCACHE_KEY = "nocache";
  private static final String NOCACHE_VALUE = "1";

  private static final String COUNTRY_KEY = "country";
  private static final String COUNTRY_VALUE = "ALL";

  private static final String LANG_KEY = "lang";
  private static final String LANG_VALUE = "ALL";

  private static final String VIEW_KEY = "view";
  private static final String VIEW_VALUE = "default";

  private static final String PERMISSION_KEY = "permission";

  private static final String URL_KEY = "url";

  private static final String PREF_PREFIX = "up_";

  private static final String SECURITY_TOKEN_KEY = "st";

  // use rpc token in shindig url for pass gadgetRef
  private static final String RPC_TOKEN = "rpctoken";

  private static final Log log = LogFactory.getLog(UrlBuilder.class);

  private static final String PARENT_KEY = "parent";

  public static String buildShindigUrl(GadgetMapper gadget) throws Exception {
    String gadgetDef = getGadgetDef(gadget.getName());
    return ServerBase.getBase() + SERVLET_PATH + "?" + getDefaultParams() + "&"
        + GADGET_ID_KEY + "=" + gadget.getShindigId() + "&" + PARENT_KEY + "="
        + ServerBase.getBase() + "&" + PERMISSION_KEY + "="
        + gadget.getPermission() + "&" + URL_KEY + "=" + gadgetDef
        + getUserPrefs(gadget.getUserPrefs()) + "&" + SECURITY_TOKEN_KEY + "="
        + getSecurityToken(gadget, gadgetDef) + "#" + RPC_TOKEN + "="
        + gadget.getId();
  }

  /**
   * Get Gadget Definition with GadgetService Gadget Definition is google xml
   * gadget
   * 
   * @param name
   * @return
   * @throws Exception
   */
  public static String getGadgetDef(String name) throws Exception {
    return Framework.getService(GadgetService.class)
        .getGadgetDefinition(name)
        .toString();
  }

  private static String getSecurityToken(GadgetMapper gadget, String url)
      throws Exception {
    return SecureTokenBuilder.getSecureToken(gadget.getViewer(),
        gadget.getOwner(), url);
  }

  /**
   * Build a url format parameters with preferences of gadget Util for render
   * gadget into Shinding opensocial server
   * 
   * @param prefs
   * @return String &up_key=value&up..
   */
  protected static String getUserPrefs(List<PreferencesBean> prefs) {
    String prefsParams = "";
    for (PreferencesBean bean : prefs) {
      String value = bean.getDefaultValue();
      if (bean.getValue() != null)
        value = bean.getValue();
      try {
        prefsParams += "&" + PREF_PREFIX + bean.getName() + "="
            + URLEncoder.encode(value, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        log.error(e);
      }
    }
    return prefsParams;
  }

  private static String getDefaultParams() {
    return CONTAINER_KEY + "=" + CONTAINER_VALUE + "&" + NOCACHE_KEY + "="
        + NOCACHE_VALUE + "&" + COUNTRY_KEY + "=" + COUNTRY_VALUE + "&"
        + LANG_KEY + "=" + LANG_VALUE + "&" + VIEW_KEY + "=" + VIEW_VALUE;
  }
}
