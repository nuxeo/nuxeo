/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.codec;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class CodecServiceImpl extends DefaultComponent implements CodecService {
    private static final Log log = LogFactory.getLog(CodecServiceImpl.class);

    public static final String CODEC_XP = "codec";

    public static final int APPLICATION_STARTED_ORDER = -600;

    protected final Map<String, CodecDescriptor> configs = new HashMap<>();

    protected final Map<String, CodecFactory> codecFactories = new HashMap<>();

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(CODEC_XP)) {
            CodecDescriptor descriptor = (CodecDescriptor) contribution;
            configs.put(descriptor.getName(), descriptor);
            log.debug(String.format("Register Codec contribution: %s", descriptor));
            codecFactories.put(descriptor.getName(), descriptor.getInstance());
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        log.debug("Deactivating service");
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        log.debug("Activating service");
    }

    @Override
    public <T> Codec<T> getCodec(String codecName, Class<T> objectClass) {
        if (!codecFactories.containsKey(codecName)) {
            return null;
        }
        // TODO codec are thread safe so we can use a cache codecName, objectClass -> codec
        return codecFactories.get(codecName).newCodec(objectClass);
    }
}
