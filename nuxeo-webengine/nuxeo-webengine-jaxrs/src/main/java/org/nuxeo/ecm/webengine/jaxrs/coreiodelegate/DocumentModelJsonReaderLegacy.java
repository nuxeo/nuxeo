/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.webengine.jaxrs.coreiodelegate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.LEGACY_MODE_READER;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.DERIVATIVE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.JsonFactoryProvider;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonParser;

/**
 * Delegates the {@link DocumentModel} Json reading to the old marshaller: JSONDocumentModelReader.
 * <p>
 * It's enable if system property nuxeo.document.json.legacy=true or if request header X-NXDocumentJsonLegacy=true.
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = DERIVATIVE)
@Supports(APPLICATION_JSON)
public class DocumentModelJsonReaderLegacy implements Reader<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentModelJsonReaderLegacy.class);

    public static final String CONF_DOCUMENT_JSON_LEGACY = "nuxeo.document.json.legacy";

    public static final String HEADER_DOCUMENT_JSON_LEGACY = "X-NXDocumentJsonLegacy";

    private static boolean IS_METHOD_LOADED = false;

    private static Method METHOD = null;

    private static void loadMethod() {
        try {
            Class<?> legacy = Class.forName("org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader");
            Method method = legacy.getMethod("readJson", JsonParser.class, MultivaluedMap.class,
                    HttpServletRequest.class);
            METHOD = method;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            log.error(
                    "Unable to find method org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader.readJson(JsonParser, MultivaluedMap<String, String>, HttpServletRequest)",
                    e);
            return;
        }
    }

    private static Boolean CONF_KEY = null;

    private static boolean getConfKey() {
        if (CONF_KEY == null) {
            CONF_KEY = Framework.isBooleanPropertyTrue(CONF_DOCUMENT_JSON_LEGACY);
        }
        return CONF_KEY;
    }

    public static void pushInstanceIfNeeded(RenderingContext ctx, HttpServletRequest request,
            MultivaluedMap<String, String> httpHeaders) {
        if (!IS_METHOD_LOADED) {
            loadMethod();
        }
        if (METHOD == null) {
            return;
        }
        String header = request.getHeader(HEADER_DOCUMENT_JSON_LEGACY);
        if (header != null) {
            try {
                boolean enable = Boolean.valueOf(header);
                if (enable) {
                    DocumentModelJsonReaderLegacy instance = new DocumentModelJsonReaderLegacy(request, httpHeaders);
                    ctx.setParameterValues(LEGACY_MODE_READER, instance);
                    return;
                } else {
                    return;
                }
            } catch (Exception e) {
                log.warn("Invalid header value for X-NXDocumentJsonLegacy : true|false");
            }
        }
        if (getConfKey()) {
            DocumentModelJsonReaderLegacy instance = new DocumentModelJsonReaderLegacy(request, httpHeaders);
            ctx.setParameterValues(LEGACY_MODE_READER, instance);
            return;
        } else {
            return;
        }
    }

    private HttpServletRequest request;

    private MultivaluedMap<String, String> httpHeaders;

    private DocumentModelJsonReaderLegacy(HttpServletRequest request, MultivaluedMap<String, String> httpHeaders) {
        super();
        this.request = request;
        this.httpHeaders = httpHeaders;
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return true;
    }

    @Override
    public DocumentModel read(Class<?> clazz, Type genericType, MediaType mediaType, InputStream in) throws IOException {
        try {
            JsonParser parser = JsonFactoryProvider.get().createJsonParser(in);
            return DocumentModel.class.cast(METHOD.invoke(null, parser, httpHeaders, request));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.error("Unable to use legacy document model reading", e);
            return null;
        }
    }

}
