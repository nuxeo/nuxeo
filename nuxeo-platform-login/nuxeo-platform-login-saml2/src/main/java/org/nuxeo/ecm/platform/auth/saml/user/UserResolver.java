/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.ecm.platform.auth.saml.user;

import java.util.Map;

import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;

/**
 * Extract a real interface for resolving SAML users to Nuxeo Users
 *
 * @author tiry
 * @since 7.4
 */

public interface UserResolver {

    void init(Map<String, String> parameters);

    String findOrCreateNuxeoUser(SAMLCredential userInfo);
}
