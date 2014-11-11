/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.jaxrs.JsonFactoryProvider;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartRequestReader;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationModule extends WebEngineModule {

    protected static final Log log = LogFactory.getLog(AutomationModule.class);

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartRequestReader.class);
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    protected static Set<Object> setupSingletons() {

        Set<Object> result = new HashSet<Object>();

        AutomationServer as = Framework.getLocalService(AutomationServer.class);

        for (Class<? extends MessageBodyReader<?>> readerKlass : as.getReaders()) {
            try {
                result.add(readerKlass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Unable to instanciate MessageBodyReader : "
                        + readerKlass, e);
            }
        }

        for (Class<? extends MessageBodyWriter<?>> writerKlass : as.getWriters()) {
            try {
                result.add(writerKlass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Unable to instanciate MessageBodyWriter : "
                        + writerKlass, e);
            }
        }

        result.add(new JsonFactoryProvider());
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        return setupSingletons();
    }

}
