/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.chemistry.opencmis.server.impl.webservices.AbstractService;
import org.apache.chemistry.opencmis.server.impl.webservices.AbstractUsernameTokenAuthHandler;

/**
 * Extracts username and password from a UsernameToken
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.3
 */
public class CXFAuthHandler extends AbstractUsernameTokenAuthHandler implements SOAPHandler<SOAPMessageContext> {

    public Set<QName> getHeaders() {
        return HEADERS;
    }

    public void close(MessageContext context) {
    }

    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean handleMessage(SOAPMessageContext context) {
        if ((Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            // we are only looking at inbound messages
            return true;
        }

        Map<String, String> callContextMap = null;

        Object[] secHeaders = context.getHeaders(WSSE_SECURITY, WSSE_CONTEXT, true);
        if (secHeaders != null && secHeaders.length > 0) {
            for (Object header : secHeaders) {
                if (!(header instanceof JAXBElement)) {
                    continue;
                }

                if (!(((JAXBElement) header).getValue() instanceof SecurityHeaderType)) {
                    continue;
                }

                callContextMap = extractUsernamePassword((JAXBElement<SecurityHeaderType>) header);
                if (callContextMap != null) {
                    break;
                }
            }
        }

        // add user and password to context
        if (callContextMap == null) {
            callContextMap = new HashMap<String, String>();
        }

        context.put(AbstractService.CALL_CONTEXT_MAP, callContextMap);
        context.setScope(AbstractService.CALL_CONTEXT_MAP, MessageContext.Scope.APPLICATION);

        return true;
    }
}
