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

package org.nuxeo.opensocial.service.api;

import java.io.File;
import java.net.ProxySelector;

import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.nuxeo.opensocial.shindig.crypto.OAuthServiceDescriptor;
import org.nuxeo.opensocial.shindig.crypto.PortalConfig;

import com.google.inject.Injector;

public interface OpenSocialService {

    /**
     * Returns our own gadget Spec Factory
     * 
     * @return
     */
    GadgetSpecFactory getGadgetSpecFactory();

    /**
     * Specify the GUICE injector to user for the service
     * 
     * @param injector
     */
    void setInjector(Injector injector);

    /**
     * Get the symetric key for the given container
     * 
     * @param defaultContainer the container name
     * @return
     */
    String getKeyForContainer(String defaultContainer);

    /**
     * Returns the proxy settings if set
     * 
     * @return
     */
    ProxySelector getProxySelector();

    /**
     * Returns a file handle to the base64 encoded key that is used to sign
     * internal requests.
     * 
     * @return
     */
    File getSigningStateKeyFile();

    /**
     * Same as above, but in byte form. This is cached.
     */
    byte[] getSigningStateKeyBytes();

    /**
     * Returns a file handle to the default private key for communicating with
     * external resources.
     * 
     * @return
     */
    File getOAuthPrivateKeyFile();

    /**
     * Get a list of the configured external service providers that we want to
     * communicate with.
     */
    OAuthServiceDescriptor[] getOAuthServices();

    /**
     * PortalConfig array that represents the contribution from the user in the
     * opensocial xp. This is normally null.
     * 
     * @return
     */
    PortalConfig[] getPortalConfig();

    /**
     * Returns the name of the private key. Most service providers don't use
     * this.
     * 
     * @return
     */
    String getOAuthPrivateKeyName();

    /**
     * Where the Oauth "return callback" shoud go. This has to be configured to
     * the name (and path) that other servers see the nuxeo instanec as.
     * 
     * @return
     */
    String getOAuthCallbackUrl();

    /**
     * Return a list of hosts to whom we *should* pass the users current
     * JSESSIONID.
     */
    String[] getTrustedHosts();

    /**
     * We have had to make this public because it must be called at exactly the
     * right time the initialization sequence. This should be called BEFORE
     * guice initialization of shindig happens.
     */
    void setupOpenSocial() throws Exception;

}
