/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

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
        if (type.getPackage().getName().startsWith("net.java.dev.webdav.jaxrs.xml.elements")) {
            return ctx;
        } else {
            return null;
        }
    }

}
