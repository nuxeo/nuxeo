/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.File;

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
 *
 */
public class WebEngineComponent extends DefaultComponent { // implements
    // ConfigurationChangedListener
    // {

    public static final ComponentName NAME = new ComponentName(
            WebEngineComponent.class.getName());

    public static final String RENDERING_EXTENSION_XP = "rendering-extension";

    public static final String RESOURCE_BINDING_XP = "resource";

    public static final String REQUEST_CONFIGURATION_XP = "request-configuration";

    public static final String GUARD_XP = "guard"; // global guards

    public static final String FORM_XP = "form";

    private static final Log log = LogFactory.getLog(WebEngineComponent.class);

    private WebEngine engine;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);

        String webDir = Framework.getProperty("org.nuxeo.ecm.web.root");
        File root = null;
        if (webDir != null) {
            root = new File(webDir);
        } else {
            root = new File(Framework.getRuntime().getHome(), "web");
        }
        root = root.getCanonicalFile();
        log.info("Using web root: " + root);

        engine = new WebEngine(new File(root, "root.war"));

        engine.start();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        engine.stop();
        engine = null;
        super.deactivate(context);
    }

    public WebEngine getEngine() {
        return engine;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor) contribution;
            PermissionService.getInstance().registerGuard(gd.getId(),
                    gd.getGuard());
        } else if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.addResourceBinding((ResourceBinding) contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor) contribution;
            try {
                engine.registerRenderingExtension(fed.name, fed.newInstance());
            } catch (Exception e) {
                throw new RuntimeServiceException(
                        "Deployment Error. Failed to contribute freemarker template extension: "
                                + fed.name);
            }
            // TODO
            // } else if (extensionPoint.endsWith(FORM_XP)) {
            // Form form = (Form)contribution;
            // engine.getFormManager().registerForm(form);
        } else if (extensionPoint.equals(REQUEST_CONFIGURATION_XP)) {
            PathDescriptor pd = (PathDescriptor) contribution;
            engine.getRequestConfiguration().addPathDescriptor(pd);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
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
        } else if (extensionPoint.equals(REQUEST_CONFIGURATION_XP)) {
            PathDescriptor pd = (PathDescriptor) contribution;
            engine.getRequestConfiguration().removePathDescriptor(pd);
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
