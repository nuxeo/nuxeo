/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * TODO remove old WebEngine references and rename WebEngine2 to WebEngine
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineComponent extends DefaultComponent { // implements
    // ConfigurationChangedListener
    // {

    public static final ComponentName NAME = new ComponentName(WebEngineComponent.class.getName());

    public static final String RENDERING_EXTENSION_XP = "rendering-extension";

    public static final String RESOURCE_BINDING_XP = "resource";

    public static final String REQUEST_CONFIGURATION_XP = "request-configuration";

    public static final String GUARD_XP = "guard"; // global guards

    public static final String FORM_XP = "form";

    private static final Log log = LogFactory.getLog(WebEngineComponent.class);

    private WebEngine engine;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);

        String webDir = Framework.getProperty("org.nuxeo.ecm.web.root");
        File root = null;
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
        log.info("Using web root: " + root);

        engine = new WebEngine(new File(root, "root.war"));

        engine.start();
    }

    @Override
    public void deactivate(ComponentContext context) {
        engine.stop();
        engine = null;
        super.deactivate(context);
    }

    public WebEngine getEngine() {
        return engine;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor) contribution;
            try {
                PermissionService.getInstance().registerGuard(gd.getId(), gd.getGuard());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.addResourceBinding((ResourceBinding) contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor) contribution;
            try {
                engine.registerRenderingExtension(fed.name, fed.newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(
                        "Deployment Error. Failed to contribute freemarker template extension: " + fed.name);
            }
            // TODO
            // } else if (extensionPoint.endsWith(FORM_XP)) {
            // Form form = (Form)contribution;
            // engine.getFormManager().registerForm(form);
        } else if (extensionPoint.equals(REQUEST_CONFIGURATION_XP)) {
            log.warn("Extension point " + REQUEST_CONFIGURATION_XP + " is obsolete since 8.4, transactions are always active");
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor) contribution;
            PermissionService.getInstance().unregisterGuard(gd.getId());
        } else if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.removeResourceBinding((ResourceBinding) contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor) contribution;
            engine.unregisterRenderingExtension(fed.name);
            // TODO
            // } else if (extensionPoint.endsWith(FORM_XP)) {
            // Form form = (Form)contribution;
            // engine.getFormManager().unregisterForm(form.getId());
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
