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

import java.util.function.Supplier;

import org.opensaml.messaging.encoder.servlet.HttpServletResponseMessageEncoder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;

/**
 * @since 2023.0
 */
public enum SAMLOutboundBinding implements SAMLBinding {

    HTTP_REDIRECT(SAMLConstants.SAML2_REDIRECT_BINDING_URI, HTTPRedirectDeflateEncoder::new);

    protected final String bindingURI;

    protected final Supplier<HttpServletResponseMessageEncoder> encoderFactory;

    SAMLOutboundBinding(String bindingURI, Supplier<HttpServletResponseMessageEncoder> encoderFactory) {
        this.bindingURI = bindingURI;
        this.encoderFactory = encoderFactory;
    }

    @Override
    public String getBindingURI() {
        return bindingURI;
    }

    public HttpServletResponseMessageEncoder newEncoder() {
        return encoderFactory.get();
    }
}
