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
package org.nuxeo.ecm.platform.auth.saml.binding;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.OutTransport;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.ws.transport.http.HTTPTransport;


public class HTTPPostBinding extends SAMLBinding {

    public HTTPPostBinding() {
        super(new HTTPPostDecoder(), null);// TODO(nfgs): new HTTPPostEncoder(velocityEngine, "saml2-post-binding-template.vm"));
    }

    public boolean supports(InTransport transport) {
        if (transport instanceof HTTPInTransport) {
            HTTPTransport t = (HTTPTransport) transport;
            return "POST".equalsIgnoreCase(t.getHTTPMethod()) && (t.getParameterValue("SAMLRequest") != null || t.getParameterValue("SAMLResponse") != null);
        } else {
            return false;
        }
    }

    public boolean supports(OutTransport transport) {
        return transport instanceof HTTPOutTransport;
    }

    public String getBindingURI() {
        return SAMLConstants.SAML2_POST_BINDING_URI;
    }
}
