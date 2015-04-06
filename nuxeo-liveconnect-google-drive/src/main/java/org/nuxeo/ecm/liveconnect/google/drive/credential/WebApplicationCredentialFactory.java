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
 */

package org.nuxeo.ecm.liveconnect.google.drive.credential;

import com.google.api.client.auth.oauth2.Credential;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;

/**
 * Credential factory for Web Applications.
 *
 * @since 7.3
 */
public class WebApplicationCredentialFactory implements CredentialFactory {

    private OAuth2ServiceProvider provider;

    public WebApplicationCredentialFactory(OAuth2ServiceProvider provider) {
        this.provider = provider;
    }

    @Override
    public Credential build(String user) {
        return provider.loadCredential(user);
    }
}
