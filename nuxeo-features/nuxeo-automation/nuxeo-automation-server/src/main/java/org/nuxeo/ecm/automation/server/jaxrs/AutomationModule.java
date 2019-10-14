/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.jaxrs.JsonFactoryProvider;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationModule extends WebEngineModule {

    protected static final Logger log = LogManager.getLogger(AutomationModule.class);

    @Override
    public Set<Class<?>> getClasses() {

        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    protected static Set<Object> setupSingletons() {

        Set<Object> result = new HashSet<>();

        AutomationServer as = Framework.getService(AutomationServer.class);

        for (Class<? extends MessageBodyReader<?>> readerKlass : as.getReaders()) {
            try {
                result.add(readerKlass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instantiate MessageBodyReader: {}", readerKlass, e);
            }
        }

        for (Class<? extends MessageBodyWriter<?>> writerKlass : as.getWriters()) {
            try {
                result.add(writerKlass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instantiate MessageBodyWriter: {}", writerKlass, e);
            }
        }

        result.add(new AutomationServiceProvider());
        result.add(new AutomationServerProvider());
        result.add(new JsonFactoryProvider());
        result.add(new CoreSessionProvider());
        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new JsonCoreIODelegate());
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        return setupSingletons();
    }

}
