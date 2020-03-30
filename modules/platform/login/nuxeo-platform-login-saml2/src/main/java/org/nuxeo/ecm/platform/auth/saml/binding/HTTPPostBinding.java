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
package org.nuxeo.ecm.platform.auth.saml.binding;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;

/**
 * HTTP Post Binding
 *
 * @since 6.0
 */
public class HTTPPostBinding extends SAMLBinding {

    public static final String SAML_REQUEST = "SAMLRequest";

    public static final String SAML_RESPONSE = "SAMLResponse";

    public HTTPPostBinding() {
        super(new HTTPPostDecoder(), null);// TODO(nfgs): add HTTPPostEncoder
    }

    @Override
    public boolean supports(InTransport transport) {
        if (transport instanceof HTTPInTransport) {
            HTTPTransport t = (HTTPTransport) transport;
            return "POST".equalsIgnoreCase(t.getHTTPMethod())
                    && (t.getParameterValue(SAML_REQUEST) != null || t.getParameterValue(SAML_RESPONSE) != null);
        } else {
            return false;
        }
    }

    @Override
    public boolean supports(OutTransport transport) {
        return transport instanceof HTTPOutTransport;
    }

    @Override
    public String getBindingURI() {
        return SAMLConstants.SAML2_POST_BINDING_URI;
    }
}
