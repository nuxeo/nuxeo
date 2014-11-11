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
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.automation.server.jaxrs.io.BlobsWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonAutomationInfoWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonDocumentListWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonDocumentWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonExceptionWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonLoginInfoWriter;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonRequestReader;
import org.nuxeo.ecm.automation.server.jaxrs.io.MultiPartRequestReader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationModule extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(AutomationResource.class);
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new JsonRequestReader());
        result.add(new JsonExceptionWriter());
        result.add(new JsonAutomationInfoWriter());
        result.add(new JsonDocumentWriter());
        result.add(new JsonDocumentListWriter());
        result.add(new BlobsWriter());
        result.add(new JsonLoginInfoWriter());
        return result;
    }

}
