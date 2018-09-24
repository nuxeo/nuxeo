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

import java.util.List;

import org.nuxeo.ecm.automation.io.services.codec.CodecDescriptor;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7.3
 */
public class IOComponent extends DefaultComponent {

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
        List<CodecDescriptor> descriptors = getDescriptors(XP_CODECS);
        descriptors.forEach(d -> {
            try {
                codecs.addCodec(d.klass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                getLog().error(e,e);
            }
        });
        codecs.postInit();
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        codecs = null;
    }

}
