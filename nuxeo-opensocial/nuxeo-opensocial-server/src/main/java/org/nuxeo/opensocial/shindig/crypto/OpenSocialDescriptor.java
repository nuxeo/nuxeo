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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.shindig.crypto;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * This class is the java-level description of the parameters that are
 * configured into the opensocial implementation. This contribution is usually
 * packaged as default-opensocial-config.xml and resides in the config
 * directory.
 *
 * The implementation of opensocial is based on shindig 1.1 and is integrated
 * inside nuxeo itself (there is no extra "shindig process").
 *
 * @author iansmith
 *
 */
@XObject("opensocial")
public class OpenSocialDescriptor {

    /**
     * This field is the key that is used by shindig to communicate with itself.
     * For example, sometimes the interpretation of a gadget results in a call
     * to the "make request" servlet for access to external resources. This
     * symmetric key is used to sign the message going from shindig to shindig
     * to verify that the message received by the make request servlet is not
     * "forged".
     * <p>
     * This value can and, in most cases should, be left empty. When it is left
     * empty, the system will use a random set of bytes for this key.
     */
    @XNode("signingKey")
    protected String signingKey;

    /**
     * This is the URL that shindig should tell other servers to use to call us
     * back on. If you have changed where nuxeo is mounted (not in /nuxeo) you
     * may need to set this to have your prefix.
     * <p>
     * If you are running nuxeo in the default configuration, you should not
     * need to configure this.
     */
    @XNode("oauthCallbackUrl")
    protected String callbackUrl;

    /**
     * This is a compatibility flag to allow JSESSIONID propagation between Shindig and Nuxeo.
     * The default value is 'false' and Signed Fetch is used.
     */
    @XNode("propagateJSESSIONIDToTrustedHosts")
    protected boolean propagateJSESSIONIDToTrustedHosts=false;

    /**
     * You can have any number of portal configurations, but most people should
     * simply ignore this.
     */
    @XNodeList(value = "portals/portalConfig", type = PortalConfig[].class, componentType = PortalConfig.class)
    protected PortalConfig[] portal;

    /**
     * This a list of nuxeo trusted hosts. Such a host will be passed the
     * browsers jsession id to avoid the need to constantly re-authenticate to
     * retrieve nuxeo data when the user is already logged into a nuxeo server
     * to access the dashboard. Will be colon separated.
     */
    @XNode("trustedHosts")
    protected String trustedHosts;

    public String getSigningKey() {
        return signingKey;
    }

    /**
     * For now, this is always null because it isn't used.
     */
    public PortalConfig[] getPortalConfig() {
        return portal;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setSigningKey(String keyAsBase64) {
        signingKey = keyAsBase64;
    }

    public String[] getTrustedHosts() {
        return trustedHosts.split(":");
    }

    /**
     * You can have any number of oauthservice configurations, but most people
     * should simply ignore this.
     */
    @XNodeList(value = "oauthservices/oauthservice", type = OAuthServiceDescriptor[].class, componentType = OAuthServiceDescriptor.class)
    protected OAuthServiceDescriptor[] services;

    public OAuthServiceDescriptor[] getOAuthServices() {
        return services;

    }

    public boolean propagateJSESSIONIDToTrustedHosts() {
        return propagateJSESSIONIDToTrustedHosts;
    }
}
