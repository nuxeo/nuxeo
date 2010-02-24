/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.PublishingMarshalingException;
import org.nuxeo.ecm.webengine.WebEngine;

@Provider
public class RemotePubMessageReader implements
        MessageBodyReader<RemotePubParam> {

    public boolean isReadable(Class arg0, Type arg1, Annotation[] arg2,
            MediaType mt) {
        return mt.equals(RemotePubParam.mediaType);
    }

    public RemotePubParam readFrom(Class arg0, Type arg1, Annotation[] arg2,
            MediaType arg3, MultivaluedMap arg4, InputStream is)
            throws IOException, WebApplicationException {

        DefaultMarshaler marshaler = new DefaultMarshaler(
                WebEngine.getActiveContext().getCoreSession());

        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String xmlData = sb.toString();

        try {
            List<Object> params = marshaler.unMarshallParameters(xmlData);
            return new RemotePubParam(params);
        } catch (PublishingMarshalingException e) {
            throw new IOException("Error while unmarshaling parameters"
                    + e.getMessage());
        }
    }
}
