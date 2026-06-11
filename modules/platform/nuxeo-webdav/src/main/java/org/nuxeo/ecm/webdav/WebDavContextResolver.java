/*
 * (C) Copyright 2010-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.nuxeo.ecm.webdav.jaxrs.Util;

/**
 * Injects the JAXBContext needed to parse our webdav XML payloads.
 */
@Provider
@Produces({ "application/xml", "text/xml" })
public class WebDavContextResolver implements ContextResolver<JAXBContext> {

    private final JAXBContext ctx;

    public WebDavContextResolver() {
        try {
            ctx = Util.getJaxbContext();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        String pkg = type.getPackage().getName();
        if (pkg.startsWith("org.jugs.webdav.jaxrs.xml") || pkg.startsWith("org.nuxeo.ecm.webdav.jaxrs")) {
            return ctx;
        } else {
            return null;
        }
    }

}
