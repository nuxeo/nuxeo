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

public abstract class SAMLBinding {

    private MessageDecoder decoder;
    private MessageEncoder encoder;

    public SAMLBinding(MessageDecoder decoder, MessageEncoder encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public void decode(MessageContext context) throws org.opensaml.xml.security.SecurityException, MessageDecodingException {
        decoder.decode(context);
    }

    public  void encode(MessageContext context) throws MessageEncodingException {
        encoder.encode(context);
    }

    public abstract String getBindingURI();
    public abstract boolean supports(InTransport transport);
    public abstract boolean supports(OutTransport transport);
}