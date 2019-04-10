/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 *      Andre Justo
 */

package org.nuxeo.ecm.liveconnect.dropbox;

import com.google.api.client.auth.oauth2.Credential;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

/**
 * Credential factory.
 *
 * @since 7.3
 */
public class OAuthCredentialFactory {

    private static final Log log = LogFactory.getLog(OAuthCredentialFactory.class);

    private OAuth2ServiceProvider provider;

    public OAuthCredentialFactory(OAuth2ServiceProvider provider) {
        this.provider = provider;
    }

    public Credential build(String user) {
        return provider.loadCredential(user);
    }
}
