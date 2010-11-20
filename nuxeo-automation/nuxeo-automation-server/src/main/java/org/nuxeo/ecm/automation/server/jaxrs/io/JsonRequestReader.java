/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import net.sf.json.JSONObject;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentRefListImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Consumes("application/json+nxrequest")
public class JsonRequestReader implements MessageBodyReader<ExecutionRequest> {

    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        // TODO check media type too
        return ExecutionRequest.class.isAssignableFrom(arg0);
    }

    public ExecutionRequest readFrom(Class<ExecutionRequest> arg0, Type arg1,
            Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> arg4, InputStream in)
            throws IOException, WebApplicationException {
        return readRequest(in);
    }

    @SuppressWarnings("unchecked")
    public static ExecutionRequest readRequest(InputStream in)
            throws IOException {
        String content = FileUtils.read(in);
        JSONObject json = JSONObject.fromObject(content);
        String input = json.optString("input", null);
        JSONObject jsonParams = json.optJSONObject("params");
        JSONObject jsonContext = json.optJSONObject("context");

        Object inObj = null;
        if (input != null) {
            if (input.startsWith("doc:")) {
                inObj = docRefFromString(input.substring(4).trim());
            } else if (input.startsWith("docs:")) {
                String[] ar = StringUtils.split(input.substring(5).trim(), ',',
                        true);
                DocumentRefList list = new DocumentRefListImpl(ar.length);
                for (String s : ar) {
                    list.add(docRefFromString(s));
                }
                inObj = list;
            }
        }
        ExecutionRequest req = new ExecutionRequest(inObj);

        if (jsonParams != null) {
            Iterator<String> it = jsonParams.keys();
            while (it.hasNext()) {
                String key = it.next();
                String value = jsonParams.getString(key);
                req.setParam(key, value);
            }
        }

        if (jsonContext != null) {
            Iterator<String> it = jsonContext.keys();
            while (it.hasNext()) {
                String key = it.next();
                String value = jsonContext.getString(key);
                req.setContextParam(key, value);
            }
        }

        return req;
    }

    public static DocumentRef docRefFromString(String input) {
        if (input.startsWith("/")) {
            return new PathRef(input);
        } else {
            return new IdRef(input);
        }
    }
}
