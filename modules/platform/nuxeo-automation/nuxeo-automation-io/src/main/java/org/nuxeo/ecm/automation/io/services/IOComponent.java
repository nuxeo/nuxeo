/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.io.services.codec.CodecDescriptor;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7.3
 */
public class IOComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(IOComponent.class);

    /**
     * @since 10.3
     */
    public static final String XP_CODECS = "codecs";

    private JsonFactoryManager jsonFactoryManager;

    private ObjectCodecService codecs;

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (ObjectCodecService.class.isAssignableFrom(adapter)) {
            return adapter.cast(codecs);
        } else if (JsonFactoryManager.class.isAssignableFrom(adapter)) {
            return adapter.cast(jsonFactoryManager);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        codecs = new ObjectCodecService(jsonFactoryManager.getJsonFactory());
        List<CodecDescriptor> descriptors = getRegistryContributions(XP_CODECS);
        for (CodecDescriptor d : descriptors) {
            try {
                Class<?> clazz = Class.forName(d.klass);
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                ObjectCodec<?> codec = (ObjectCodec<?>) constructor.newInstance();
                codecs.addCodec(codec);
            } catch (ClassCastException | ReflectiveOperationException e) {
                String msg = String.format("Failed to register codec on '%s': error initializing class '%s' (%s).",
                        name, d.getId(), e.toString());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
            }
        }
        codecs.postInit();
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        codecs = null;
    }

}
