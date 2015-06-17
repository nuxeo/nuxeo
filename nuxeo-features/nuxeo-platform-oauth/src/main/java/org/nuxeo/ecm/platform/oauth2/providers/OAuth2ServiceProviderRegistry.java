/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import java.util.List;

/**
 * This service is used to manage OAuth2 Service Providers
 */
public interface OAuth2ServiceProviderRegistry {

    OAuth2ServiceProvider getProvider(String serviceName);

    OAuth2ServiceProvider addProvider(String serviceName, String description, String tokenServerURL, String authorizationServerURL,
            String clientId, String clientSecret, List<String> scopes);
}
