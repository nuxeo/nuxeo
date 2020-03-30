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

import java.net.MalformedURLException;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLRuntimeException;
import org.opensaml.saml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.saml.common.binding.encoding.SAMLMessageEncoder;

import net.shibboleth.utilities.java.support.net.SimpleURLCanonicalizer;
import net.shibboleth.utilities.java.support.net.URIComparator;
import net.shibboleth.utilities.java.support.net.URIException;

/**
 * Based class for SAML bindings, used for parsing messages.
 *
 * @since 6.0
 */
public abstract class SAMLBinding {

    protected SAMLMessageDecoder decoder;

    protected SAMLMessageEncoder encoder;

    /**
     * URIComparator that strips scheme to avoid issues with reverse proxies
     */
    public static final URIComparator uriComparator = new URIComparator() {
        @Override
        public boolean compare(String uri1, String uri2) throws URIException {
            if (uri1 == null && uri2 == null) {
                return true;
            } else if (uri1 == null || uri2 == null) {
                return false;
            } else {
                try {
                    String uri1Canon = SimpleURLCanonicalizer.canonicalize(uri1).replaceFirst("^(https:|http:)", "");
                    String uri2Canon = SimpleURLCanonicalizer.canonicalize(uri2).replaceFirst("^(https:|http:)", "");
                    return uri1Canon.equals(uri2Canon);
                } catch (MalformedURLException e) {
                    throw new URIException(e);
                }
            }
        }
    };

    public SAMLBinding(SAMLMessageDecoder decoder, SAMLMessageEncoder encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
        // NXP-17044: strips scheme to fix validity check with reverse proxies
        if (decoder != null) {
//            ((BaseSAMLMessageDecoder) decoder).setURIComparator(uriComparator);
        }
    }

    /**
     * Decodes the given message.
     *
     * @param context the message to decode
     * @throws org.opensaml.xml.security.SecurityException
     * @throws MessageDecodingException
     */
    public void decode(MessageContext context) throws org.opensaml.security.SecurityException,
            MessageDecodingException {
        decoder.decode(context);
    }

    /**
     * Encodes the given message.
     *
     * @param context the message to encode
     * @throws MessageEncodingException
     */
    public void encode(MessageContext context) throws MessageEncodingException {
        encoder.encode(context);
    }

    /**
     * Returns the URI that identifies this binding.
     *
     * @return the URI
     */
    public abstract String getBindingURI();

    /**
     * Checks if this binding can be used to extract the message from the request.
     *
     * @param transport
     * @return true if this binding supports the transport
     */
    public abstract boolean supports(InTransport transport);

    /**
     * Checks if this binding can use the given transport to send a message
     *
     * @param transport
     * @return true if this binding supports the transport
     */
    public abstract boolean supports(OutTransport transport);
}
