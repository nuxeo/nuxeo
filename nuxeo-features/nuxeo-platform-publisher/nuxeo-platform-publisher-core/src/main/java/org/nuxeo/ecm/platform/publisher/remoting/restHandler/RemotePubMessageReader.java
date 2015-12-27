/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.webengine.WebEngine;

@Provider
public class RemotePubMessageReader implements MessageBodyReader<RemotePubParam> {

    public boolean isReadable(Class arg0, Type arg1, Annotation[] arg2, MediaType mt) {
        return mt.equals(RemotePubParam.mediaType);
    }

    public RemotePubParam readFrom(Class arg0, Type arg1, Annotation[] arg2, MediaType arg3, MultivaluedMap arg4,
            InputStream is) throws IOException, WebApplicationException {

        DefaultMarshaler marshaler = new DefaultMarshaler(WebEngine.getActiveContext().getCoreSession());

        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String xmlData = sb.toString();

        List<Object> params = marshaler.unMarshallParameters(xmlData);
        return new RemotePubParam(params);
    }

}
