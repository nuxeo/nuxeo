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

import org.nuxeo.ecm.automation.io.services.codec.CodecDescriptor;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7.3
 */
public class IOComponent extends DefaultComponent {

    protected static final String XP_CODECS = "codecs";

    private JsonFactoryManager jsonFactoryManager;

    private ObjectCodecService codecs;

    @Override
    public void activate(ComponentContext context) {
        jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        codecs = new ObjectCodecService(jsonFactoryManager.getJsonFactory());
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_CODECS.equals(extensionPoint)) {
            CodecDescriptor codec = (CodecDescriptor) contribution;
            codecs.addCodec(codec.newInstance());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_CODECS.equals(extensionPoint)) {
            CodecDescriptor codec = (CodecDescriptor) contribution;
            codecs.removeCodec(codec.newInstance().getJavaType());
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (ObjectCodecService.class.isAssignableFrom(adapter)) {
            return adapter.cast(codecs);
        } else if (JsonFactoryManager.class.isAssignableFrom(adapter)) {
            return adapter.cast(jsonFactoryManager);
        }
        return null;
    }

    @Override
    public void start(ComponentContext context) {
        codecs.postInit();
    }

}
