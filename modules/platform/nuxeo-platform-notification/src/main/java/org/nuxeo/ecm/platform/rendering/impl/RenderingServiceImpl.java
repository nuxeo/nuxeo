/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dragos
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.rendering.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of RenderingService
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class RenderingServiceImpl extends DefaultComponent implements RenderingService {

    private static final Logger log = LogManager.getLogger(RenderingServiceImpl.class);

    public static final String EP_RENDER_ENGINES = "engines";

    private Map<String, RenderingEngine> engines;

    @Override
    public void start(ComponentContext context) {
        engines = new ConcurrentHashMap<>();
        this.<RenderingEngineDescriptor> getRegistryContributions(EP_RENDER_ENGINES).forEach(desc -> {
            try {
                RenderingEngine engine = desc.newInstance();
                engines.put(desc.getFormat(), engine);
            } catch (ReflectiveOperationException e) {
                String message = String.format("Cannot register rendering engine for %s", desc.getFormat());
                addRuntimeMessage(Level.ERROR, message);
                log.error(message, e);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        engines = null;
    }

    @Override
    public RenderingEngine getEngine(String name) {
        return engines.get(name);
    }

    @Override
    public Collection<RenderingResult> process(RenderingContext renderingCtx) throws RenderingException {
        List<RenderingResult> ret = new ArrayList<>();

        for (RenderingEngine engine : engines.values()) {
            if (renderingCtx.accept(engine)) {
                RenderingResult result = engine.process(renderingCtx);
                if (result != null) {
                    ret.add(result);
                } else if (log.isDebugEnabled()) {
                    log.debug("rendering ignored by the engine {}", engine::getFormatName);
                }
            }
        }
        return ret;
    }

    @Override
    public void registerEngine(RenderingEngine engine) {
        RenderingEngine existing = engines.put(engine.getFormatName(), engine);
        if (existing != null) {
            log.debug("Replaced existing RenderingEngine {}", engine::getFormatName);
        } else if (log.isDebugEnabled()) {
            log.debug("Registered RenderingEngine {}", engine::getFormatName);
        }
    }

    @Override
    public void unregisterEngine(String name) {
        engines.remove(name);
    }

}
