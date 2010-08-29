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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Guillaume Cusnieux
 */
public class SecureTokenBuilder {

    private static final Log log = LogFactory.getLog(SecureTokenBuilder.class);

    public static String getSecureToken(String viewer, String owner,
            String gadgetUrl) throws Exception {
        OpenSocialService svc = Framework.getService(OpenSocialService.class);
        String container = "default";
        String domain = "localhost";
        if (svc.getPortalConfig() == null) {
            log.warn("portal configuration suggests that there are "
                    + svc.getPortalConfig().length
                    + " choices but we don't know how to pick the correct configuration!");
        }
        return getSecureToken(viewer, owner, gadgetUrl,
                svc.getSigningStateKeyBytes(), container, domain);
    }

    /**
     * XXX LeroyMerlin's old version.
     *
     * @param viewer
     * @param owner
     * @param gadgetUrl
     * @return
     * @throws Exception
     */
    // private static String XXXgetSecureToken(String viewer, String owner,
    // String gadgetUrl) throws Exception {

    // PortalConfig config = PortalComponent.getInstance().getConfig();

    // String key = config.getKey();
    // String container = config.getContainerName();
    // String domain = config.getDomain();

    // return getSecureToken(viewer, owner, gadgetUrl, key, container,
    // domain);

    // }

    public static String getSecureToken(String viewer, String owner,
            String gadgetUrl, byte[] key, String container, String domain)
            throws Exception {
        BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(
                new BasicBlobCrypter(key), container, domain);
        st.setViewerId(viewer);
        st.setOwnerId(owner);
        st.setAppUrl(gadgetUrl);
        return Utf8UrlCoder.encode(st.encrypt());

    }

    /**
     * XXX LeroyMerlin's old version.
     */
    private static String XXXgetSecureToken(String viewer, String owner,
            String gadgetUrl, String key, String container, String domain)
            throws Exception {
        BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(
                new BasicBlobCrypter(key.getBytes()), container, domain);
        st.setViewerId(viewer);
        st.setOwnerId(owner);
        st.setAppUrl(gadgetUrl);
        return Utf8UrlCoder.encode(st.encrypt());
    }

}
