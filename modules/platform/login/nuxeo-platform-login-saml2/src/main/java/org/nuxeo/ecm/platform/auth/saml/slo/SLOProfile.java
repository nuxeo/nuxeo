/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml.slo;

import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.metadata.Endpoint;

/**
 * WebSLO (Single Log Out) profile.
 *
 * @since 6.0
 */
public interface SLOProfile {

    /**
     * Identifier of the Single Logout profile.
     */
    String PROFILE_URI = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:logout";

    LogoutRequest buildLogoutRequest(MessageContext<SAMLObject> context, SAMLCredential credential) throws SAMLException;

    void processLogoutResponse(MessageContext<SAMLObject> context) throws SAMLException;

    boolean processLogoutRequest(MessageContext<SAMLObject> context, SAMLCredential credential) throws SAMLException;

    Endpoint getEndpoint();
}
