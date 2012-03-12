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
import com.google.inject.Injector;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.nuxeo.opensocial.shindig.crypto.OAuthServiceDescriptor;
import org.nuxeo.opensocial.shindig.crypto.PortalConfig;

public interface OpenSocialService {

    /**
     * Returns our own gadget Spec Factory
     */
    GadgetSpecFactory getGadgetSpecFactory();

    /**
     * Specify the GUICE injector to user for the service
     */
    void setInjector(Injector injector);

    /**
     * Returns a file handle to the base64 encoded key that is used to sign
     * internal requests.
     */
    File getSigningStateKeyFile();

    /**
     * Same as above, but in byte form. This is cached.
     */
    byte[] getSigningStateKeyBytes();

    /**
     * Get a list of the configured external service providers that we want to
     * communicate with.
     */
    OAuthServiceDescriptor[] getOAuthServices();

    /**
     * PortalConfig array that represents the contribution from the user in the
     * opensocial xp. This is normally null.
     */
    PortalConfig[] getPortalConfig();

    /**
     * Where the Oauth "return callback" should go. This has to be configured to
     * the name (and path) that other servers see the nuxeo instance as.
     */
    String getOAuthCallbackUrl();

    /**
     * Return a list of hosts that can be considered as trusted : i.e : we can
     * use internal sign fetch or propagate JSESSIONID
     */
    String[] getTrustedHosts();

    /**
     * Returns {@code true} if the given host is a trusted host, {@code false}
     * otherwise.
     */
    boolean isTrustedHost(String host);

    /**
     * We have had to make this public because it must be called at exactly the
     * right time the initialization sequence. This should be called BEFORE
     * guice initialization of shindig happens.
     */
    void setupOpenSocial() throws Exception;

    /**
     * For communication between Shindig and Nuxeo we have 2 options : - use
     * Signed Fetch based on a dynamically generated shared key - propagate the
     * JSESSIONID
     *
     * Since propagating JSESSIONID is a "hack" and requires specific code in
     * the gadget JS, the default is false.
     *
     * @return
     */
    boolean propagateJSESSIONIDToTrustedHosts();

}
