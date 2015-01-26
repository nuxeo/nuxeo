/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.webengine.jaxrs.coreiodelegate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;

/**
 * A JAX-RS {@link MessageBodyWriter} that try to delegate marshalling to all nuxeo-core-io {@link Writer} and
 * {@link Reader}.
 *
 * @since 7.2
 */
@Provider
@Produces({ APPLICATION_JSON, APPLICATION_JSON + "+nxrequest", APPLICATION_JSON + "+nxentity" })
public final class JsonCoreIODelegate extends PartialCoreIODelegate {

    @Override
    protected boolean accept(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

}
