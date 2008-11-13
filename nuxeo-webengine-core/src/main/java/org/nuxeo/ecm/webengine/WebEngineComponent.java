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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.install.Installer;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deploy.ConfigurationDeployer;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * TODO remove old WebEngine references and rename WebEngine2 to WebEngine
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineComponent extends DefaultComponent { //implements ConfigurationChangedListener {

    public static final ComponentName NAME = new ComponentName(WebEngineComponent.class.getName());

    public static final String RENDERING_EXTENSION_XP = "rendering-extension";
    public static final String WEB_OBJ_XP = "webObject";
    public static final String BINDING_XP = "binding"; // TODO deprecated
    public static final String RESOURCE_BINDING_XP = "resource";
    public static final String GUARD_XP = "guard"; // global guards
    public static final String APPLICATION_XP = "application";
    public static final String INSTALL_XP = "install";
    public static final String CONFIG_XP = "configuration";
    public static final String APP_MAPPING_XP = "application-mapping";
    public static final String FORM_XP = "form";


    private static final Log log = LogFactory.getLog(WebEngineComponent.class);

    private WebEngine engine;
//    private FileChangeNotifier notifier;

    private ConfigurationDeployer deployer;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);

        //TODO: this should be moved into runtime - loads annotations from current bundle
        //TODO: move this into runtime
        context.getRuntimeContext().getBundle().getBundleContext().addBundleListener(BundleAnnotationsLoader.getInstance());
        BundleAnnotationsLoader.getInstance().loadAnnotations(context.getRuntimeContext().getBundle());


        String webDir = Framework.getProperty("org.nuxeo.ecm.web.root");
        File root = null;
        if (webDir != null) {
            root = new File(webDir);
        } else {
            root = new File(Framework.getRuntime().getHome(), "web");
        }
        root = root.getCanonicalFile();
        log.info("Using web root: "+root);

        try {
            deployWebDir(context.getRuntimeContext().getBundle(), root);
        } catch (Exception e) { // delete incomplete files
            FileUtils.deleteTree(root);
            throw e;
        }

        // load message bundle
        //TODO: remove notifier
//        notifier = new FileChangeNotifier();
//        notifier.start();

        ResourceRegistry registry = Framework.getLocalService(ResourceRegistry.class);
        if (registry == null) {
            throw new Error("Could not find a server implementation");
        }
        engine = new WebEngine(registry, root);
//        deployer = new ConfigurationDeployer(notifier);
//        deployer.addConfigurationChangedListener(this);

    }


    @Override
    public void deactivate(ComponentContext context) throws Exception {
        //TODO: move this in runtime
        context.getRuntimeContext().getBundle().getBundleContext().removeBundleListener(BundleAnnotationsLoader.getInstance());

//        notifier.stop();
//        deployer.removeConfigurationChangedListener(this);
        deployer = null;
//        notifier = null;
        super.deactivate(context);
    }

    private static void deployWebDir(Bundle bundle, File root) throws IOException {
        if (!new File(root, "admin").exists()) {
            root.mkdirs();
            Installer.copyResources(bundle, "web", root);
        }
    }

    public WebEngine getEngine2() {
        return engine;
    }

    public void loadConfiguration(RuntimeContext context, File file, boolean trackChanges) throws Exception {
        try {
            deployer.deploy(context, file, trackChanges);
        } finally {
            //TODO engine2 ?
            //engine.fireConfigurationChanged();
        }
    }

    public void unloadConfiguration(File file) throws Exception {
        try {
            deployer.undeploy(file);
        } finally {
            //TODO engine2 ?
            //engine.fireConfigurationChanged();
        }
    }


    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().registerGuard(gd.getId(), gd.getGuard());
        } else if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.addResourceBinding((ResourceBinding)contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor)contribution;
            try {
                engine.registerRenderingExtension(fed.name, fed.newInstance());
            } catch (Exception e) {
                throw new RuntimeServiceException(
                        "Deployment Error. Failed to contribute freemarker template extension: "+fed.name);
            }
        } else if (extensionPoint.equals(INSTALL_XP)) {
            Installer installer = (Installer)contribution;
            installer.install(contributor.getContext(), engine.getRootDirectory());
        } else if (extensionPoint.equals(CONFIG_XP)) {
            System.out.println("Extensions point "+CONFIG_XP+" is no more supported");
//            ConfigurationFileDescriptor cfg = (ConfigurationFileDescriptor)contribution;
//            if (cfg.path != null) {
//                loadConfiguration(contributor.getContext(), new File(engine.getRootDirectory(), cfg.path), cfg.trackChanges);
//            } else if (cfg.entry != null) {
//                throw new UnsupportedOperationException("Entry is not supported for now");
//            } else {
//                log.error("Neither path neither entry attribute was defined in the configuration extension. Ignoring");
//            }
//TODO
//        } else if (extensionPoint.endsWith(FORM_XP)) {
//            Form form = (Form)contribution;
//            engine.getFormManager().registerForm(form);
        }
    }


    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().unregisterGuard(gd.getId());
        } else if (RESOURCE_BINDING_XP.equals(extensionPoint)) {
            engine.removeResourceBinding((ResourceBinding)contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor)contribution;
            engine.unregisterRenderingExtension(fed.name);
        } else if (extensionPoint.equals(INSTALL_XP)) {
            Installer installer = (Installer)contribution;
            installer.uninstall(contributor.getContext(), engine.getRootDirectory());
        } else if (extensionPoint.equals(CONFIG_XP)) {
//            ConfigurationFileDescriptor cfg = (ConfigurationFileDescriptor)contribution;
//            if (cfg.path != null) {
//                unloadConfiguration(new File(engine.getRootDirectory(), cfg.path));
//            } else if (cfg.entry != null) {
//                throw new UnsupportedOperationException("Entry is not supported for now");
//            } else {
//                log.error("Neither path neither entry attribute was defined in the configuration extension. Ignoring");
//            }
//TODO
//        } else if (extensionPoint.endsWith(FORM_XP)) {
//            Form form = (Form)contribution;
//            engine.getFormManager().unregisterForm(form.getId());
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == WebEngine.class) {
            return adapter.cast(engine);
//        } else if (adapter == FileChangeNotifier.class) {
//            return adapter.cast(notifier);
        }
        return null;
    }


//    public void configurationChanged(Entry entry) throws Exception {
//        if (engine != null) {
//            engine.reload();
//            //engine.fireConfigurationChanged(); ?
//        }
//    }

}
