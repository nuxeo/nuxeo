/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.processor.binding;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.opensaml.messaging.decoder.servlet.HttpServletRequestMessageDecoder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPRedirectDeflateDecoder;

/**
 * @since 2023.0
 */
public enum SAMLInboundBinding implements SAMLBinding {

    HTTP_POST(SAMLConstants.SAML2_POST_BINDING_URI,
            request -> "POST".equalsIgnoreCase(request.getMethod()) && iSAMLObjectPresent(request),
            HTTPPostDecoder::new), //
    HTTP_REDIRECT(SAMLConstants.SAML2_REDIRECT_BINDING_URI,
            request -> "GET".equalsIgnoreCase(request.getMethod()) && iSAMLObjectPresent(request),
            HTTPRedirectDeflateDecoder::new);

    public static final String SAML_REQUEST = "SAMLRequest";

    public static final String SAML_RESPONSE = "SAMLResponse";

    protected final String bindingURI;

    protected final Predicate<HttpServletRequest> acceptor;

    protected final Supplier<HttpServletRequestMessageDecoder> decoderFactory;

    SAMLInboundBinding(String bindingURI, Predicate<HttpServletRequest> acceptor,
            Supplier<HttpServletRequestMessageDecoder> decoderFactory) {
        this.bindingURI = bindingURI;
        this.acceptor = acceptor;
        this.decoderFactory = decoderFactory;
    }

    @Override
    public String getBindingURI() {
        return bindingURI;
    }

    /**
     * @return whether this binding can treat the given request
     */
    public boolean accept(HttpServletRequest request) {
        return acceptor.test(request);
    }

    public HttpServletRequestMessageDecoder newDecoder() {
        return decoderFactory.get();
    }

    protected static boolean iSAMLObjectPresent(HttpServletRequest request) {
        return request.getParameter(SAML_REQUEST) != null || request.getParameter(SAML_RESPONSE) != null;
    }
}
