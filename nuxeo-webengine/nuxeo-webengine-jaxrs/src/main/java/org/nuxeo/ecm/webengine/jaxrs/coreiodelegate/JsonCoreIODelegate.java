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
@Produces({ APPLICATION_JSON, APPLICATION_JSON + "+nxrequest" })
public final class JsonCoreIODelegate extends PartialCoreIODelegate {

    @Override
    protected boolean accept(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

}
