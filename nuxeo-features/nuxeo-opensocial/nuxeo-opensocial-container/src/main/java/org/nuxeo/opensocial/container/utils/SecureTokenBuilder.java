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

package org.nuxeo.opensocial.container.utils;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.nuxeo.opensocial.container.component.PortalComponent;
import org.nuxeo.opensocial.container.component.PortalConfig;

public class SecureTokenBuilder {

  public static String getSecureToken(String viewer, String owner, String gadgetUrl)
      throws Exception {

    PortalConfig config = PortalComponent.getInstance()
        .getConfig();

    String key = config.getKey();
    String container = config.getContainerName();
    String domain = config.getDomain();

    return getSecureToken(viewer, owner, gadgetUrl, key, container, domain);

  }

  private static String getSecureToken(String viewer, String owner,
      String gadgetUrl, String key, String container, String domain)
      throws Exception {
    BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(new BasicBlobCrypter(key.getBytes()),
        container, domain);
    st.setViewerId(viewer);
    st.setOwnerId(owner);
    st.setAppUrl(gadgetUrl);
    return Utf8UrlCoder.encode(st.encrypt());
  }

}
