/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dragos
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.rendering.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.RenderingService;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of RenderingService
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class RenderingServiceImpl extends DefaultComponent implements
        RenderingService {

    private static final Log log = LogFactory.getLog(RenderingServiceImpl.class);

    public static final String EP_RENDER_ENGINES = "engines";

    private final Map<String, RenderingEngine> engines = new HashMap<String, RenderingEngine>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (log.isDebugEnabled()) {
            log.debug("register: " + contribution + ", ep: " + extensionPoint);
        }

        if (extensionPoint.equals(EP_RENDER_ENGINES)) {
            RenderingEngineDescriptor desc = (RenderingEngineDescriptor) contribution;

            try {
                RenderingEngine engine = desc.newInstance();
                engines.put(desc.getFormat(), engine);
            } catch (Exception e) {
                log.error("Cannot register rendering engine for "
                        + desc.getFormat(), e);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(EP_RENDER_ENGINES)) {
            RenderingEngineDescriptor desc = (RenderingEngineDescriptor) contribution;
            engines.remove(desc.getFormat());
        }
    }

    public RenderingEngine getEngine(String name) {
        return engines.get(name);
    }

    public Collection<RenderingResult> process(RenderingContext renderingCtx)
            throws RenderingException {
        List<RenderingResult> ret = new ArrayList<RenderingResult>();

        for (RenderingEngine engine : engines.values()) {
            if (renderingCtx.accept(engine)) {
                RenderingResult result = engine.process(renderingCtx);
                if (result != null) {
                    ret.add(result);
                } else if (log.isDebugEnabled()) {
                    log.debug("rendering ignored by the engine "+ engine.getFormatName());
                }
            }
        }
        return ret;
    }

    public void registerEngine(RenderingEngine engine) {
        RenderingEngine existing = engines.put(engine.getFormatName(), engine);
        if (existing != null) {
            log.debug("Replaced existing RenderingEngine " +
                    engine.getFormatName());
        } else if (log.isDebugEnabled()) {
            log.debug("Registered RenderingEngine " + engine.getFormatName());
        }
    }

    public void unregisterEngine(String name) {
        engines.remove(name);
    }

}
