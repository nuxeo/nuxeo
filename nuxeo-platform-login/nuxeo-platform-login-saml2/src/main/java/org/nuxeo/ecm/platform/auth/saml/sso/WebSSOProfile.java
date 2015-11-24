/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.sso;

import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.opensaml.common.SAMLException;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.metadata.Endpoint;

import javax.servlet.http.HttpServletRequest;

/**
 * WebSSO (Single Sign On) profile.
 *
 * @since 6.0
 */
public interface WebSSOProfile {

    /**
     * Identifier of the WebSSO profile.
     */
    public static final String PROFILE_URI = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:browser";

    SAMLCredential processAuthenticationResponse(SAMLMessageContext context)
            throws SAMLException;

    AuthnRequest buildAuthRequest(HttpServletRequest request)
            throws SAMLException;

    Endpoint getEndpoint();

}
