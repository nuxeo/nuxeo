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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.restapi.server.BodyFactory;

/**
 *
 * @since 7.1
 */
public class RoutingBodyExtension implements BodyFactory {

    @Override
    public Set<? extends MessageBodyReader<?>> getMessageBodyReaders() {
       return Collections.singleton(new RoutingRequestReader());
    }

    @Override
    public Set<? extends MessageBodyWriter<?>> getMessageBodyWriters() {
        return Collections.emptySet();
    }

    @Override
    public int getPriority() {
        return 100;
    }

}
