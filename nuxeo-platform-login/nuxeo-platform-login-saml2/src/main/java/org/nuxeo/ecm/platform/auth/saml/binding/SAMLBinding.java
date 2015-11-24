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

import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.message.decoder.MessageDecoder;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncoder;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.OutTransport;

/**
 * Based class for SAML bindings, used for parsing messages.
 *
 * @since 6.0
 */
public abstract class SAMLBinding {

    private MessageDecoder decoder;

    private MessageEncoder encoder;

    public SAMLBinding(MessageDecoder decoder, MessageEncoder encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    /**
     * Decodes the given message.
     *
     * @param context the message to decode
     * @throws org.opensaml.xml.security.SecurityException
     * @throws MessageDecodingException
     */
    public void decode(MessageContext context)
            throws org.opensaml.xml.security.SecurityException,
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