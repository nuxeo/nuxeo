/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component for {@link WebEngine} configuration.
 */
public class WebEngineComponent extends DefaultComponent {

    private static final Logger log = LogManager.getLogger(WebEngineComponent.class);

    public static final ComponentName NAME = new ComponentName(WebEngineComponent.class.getName());

    public static final String RENDERING_EXTENSION_XP = "rendering-extension";

    public static final String RESOURCE_BINDING_XP = "resource";

    public static final String REQUEST_CONFIGURATION_XP = "request-configuration";

    public static final String GUARD_XP = "guard"; // global guards

    public static final String FORM_XP = "form";

    private WebEngine engine;

    private Set<String> renderingExtensions;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);

        String webDir = Framework.getProperty("org.nuxeo.ecm.web.root");
        File root;
        if (webDir != null) {
            root = new File(webDir);
        } else {
            root = new File(Framework.getRuntime().getHome(), "web");
        }
        try {
            root = root.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Using web root: '{}'", root);

        engine = new WebEngine(new File(root, "root.war"));
    }

    @Override
    public void deactivate(ComponentContext context) {
        engine = null;
        super.deactivate(context);
    }

    @Override
    public void start(ComponentContext context) {
        this.<GuardDescriptor> getRegistryContributions(GUARD_XP).forEach(gd -> {
            try {
                PermissionService.getInstance().registerGuard(gd.getId(), gd.getGuard());
            } catch (ParseException e) {
                String msg = String.format("Error registering guard '%s': %s", gd.getId(), e.getMessage());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
            }
        });
        renderingExtensions = new HashSet<>();
        this.<RenderingExtensionDescriptor> getRegistryContributions(RENDERING_EXTENSION_XP).forEach(fed -> {
            try {
                engine.registerRenderingExtension(fed.name, fed.newInstance());
                renderingExtensions.add(fed.name);
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Error contributing freemarker template extension '%s': %s", fed.name,
                        e.getMessage());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
            }
        });

        engine.start();
    }

    @Override
    public void stop(ComponentContext context) {
        PermissionService.getInstance().clearGuards();
        renderingExtensions.forEach(engine::unregisterRenderingExtension);
        renderingExtensions = null;
        engine.stop();
    }

    public WebEngine getEngine() {
        return engine;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            addRuntimeMessage(Level.WARNING, String.format(
                    "Extension point '%s' is obsolete since 5.8: use a JAX-RS application to declare more resources.",
                    RESOURCE_BINDING_XP), Source.EXTENSION, contributor.getName().getName());
            engine.addResourceBinding((ResourceBinding) contribution);
        } else if (extensionPoint.equals(REQUEST_CONFIGURATION_XP)) {
            addRuntimeMessage(Level.WARNING,
                    String.format("Extension point '%s' is obsolete since 8.4, transactions are always active.",
                            REQUEST_CONFIGURATION_XP),
                    Source.EXTENSION, contributor.getName().getName());
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.removeResourceBinding((ResourceBinding) contribution);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == WebEngine.class) {
            return adapter.cast(engine);
        }
        return null;
    }

}
