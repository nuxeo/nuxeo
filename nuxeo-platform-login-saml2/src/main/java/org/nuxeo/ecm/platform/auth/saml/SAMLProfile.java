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
package org.nuxeo.ecm.platform.auth.saml;

import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.xml.signature.SignatureTrustEngine;

public interface SAMLProfile {
    String getProfileIdentifier();
    Endpoint getEndpoint();

    void setTrustEngine(SignatureTrustEngine trustEngine);
    void setDecrypter(Decrypter decrypter);
}
