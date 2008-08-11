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
import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.install.Installer;
import org.nuxeo.ecm.webengine.rendering.RenderingExtensionDescriptor;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.deploy.ConfigurationChangedListener;
import org.nuxeo.runtime.deploy.ConfigurationDeployer;
import org.nuxeo.runtime.deploy.ContributionManager;
import org.nuxeo.runtime.deploy.FileChangeListener;
import org.nuxeo.runtime.deploy.FileChangeNotifier;
import org.nuxeo.runtime.deploy.ManagedComponent;
import org.nuxeo.runtime.deploy.ConfigurationDeployer.Entry;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineComponent extends ManagedComponent implements FileChangeListener, ConfigurationChangedListener {

    public static final ComponentName NAME = new ComponentName(WebEngineComponent.class.getName());

    public static final String RENDERING_EXTENSION_XP = "rendering-extension";
    public static final String WEB_OBJ_XP = "webObject";
    public static final String BINDING_XP = "binding";
    public static final String GUARD_XP = "guard"; // global guards
    public static final String APPLICATION_XP = "application";
    public static final String INSTALL_XP = "install";
    public static final String CONFIG_XP = "configuration";
    public static final String APP_MAPPING_XP = "application-mapping";
    public static final String FORM_XP = "form";


    private static final Log log = LogFactory.getLog(WebEngineComponent.class);

    private WebEngine engine;
    private FileChangeNotifier notifier;
    private ComponentContext ctx;

    private ConfigurationDeployer deployer;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        ctx = context;
        String webDir = Framework.getProperty("org.nuxeo.ecm.web.root");
        File root = null;
        if (webDir != null) {
            root = new File(webDir);
        } else {
            root = new File(Framework.getRuntime().getHome(), "web");
        }
        root = root.getCanonicalFile();
        log.info("Using web root: "+root);
        if (!new File(root, "default").exists()) {
            try {
                root.mkdirs();
                // runtime predeployment is not supporting conditional unziping so we do the predeployment here:
                deployWebDir(context.getRuntimeContext().getBundle(), root);
            } catch (Exception e) { // delete incomplete files
                FileUtils.deleteTree(root);
                throw e;
            }
        }
        // register contrib managers
        registerContributionManager(APPLICATION_XP, new ContributionManager(this));
        registerContributionManager(WEB_OBJ_XP, new ContributionManager(this));

        // load message bundle
        notifier = new FileChangeNotifier();
        notifier.start();
        notifier.addListener(this);

        engine = new DefaultWebEngine(root, notifier);
        deployer = new ConfigurationDeployer(notifier);
        deployer.addConfigurationChangedListener(this);

    }


    @Override
    public void deactivate(ComponentContext context) throws Exception {
        notifier.stop();
        notifier.removeListener(this);
        deployer.removeConfigurationChangedListener(this);
        engine.destroy();
        engine = null;
        deployer = null;
        notifier = null;
        ctx = null;
        super.deactivate(context);
    }

    private static void deployWebDir(Bundle bundle, File root) throws IOException {
        Installer.copyResources(bundle, "web", root);
    }

    public WebEngine getEngine() {
        return engine;
    }

    public void loadConfiguration(RuntimeContext context, File file, boolean trackChanges) throws Exception {
        try {
            deployer.deploy(context, file, trackChanges);
        } finally {
            engine.fireConfigurationChanged();
        }
    }

    public void unloadConfiguration(File file) throws Exception {
        try {
            deployer.undeploy(file);
        } finally {
            engine.fireConfigurationChanged();
        }
    }



    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().registerGuard(gd.getId(), gd.getGuard());
        } else if (BINDING_XP.equals(extensionPoint)) {
            WebObjectBindingDescriptor binding = (WebObjectBindingDescriptor)contribution;
            engine.registerBinding(binding.type, binding.objectId);
        } else if (APP_MAPPING_XP.equals(extensionPoint)) {
            engine.addApplicationMapping((WebApplicationMapping)contribution);
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
            ConfigurationFileDescriptor cfg = (ConfigurationFileDescriptor)contribution;
            if (cfg.path != null) {
                loadConfiguration(contributor.getContext(), new File(engine.getRootDirectory(), cfg.path), cfg.trackChanges);
            } else if (cfg.entry != null) {
                throw new UnsupportedOperationException("Entry is not supported for now");
            } else {
                log.error("Neither path neither entry attribute was defined in the configuration extension. Ignoring");
            }
        } else if (extensionPoint.endsWith(FORM_XP)) {
            Form form = (Form)contribution;
            engine.getFormManager().registerForm(form);
        }
    }


    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().unregisterGuard(gd.getId());
        } else if (BINDING_XP.equals(extensionPoint)) {
            WebObjectBindingDescriptor binding = (WebObjectBindingDescriptor)contribution;
            engine.unregisterBinding(binding.type);
        } else if (APP_MAPPING_XP.equals(extensionPoint)) {
            engine.removeApplicationMapping((WebApplicationMapping)contribution);
        } else if (extensionPoint.equals(RENDERING_EXTENSION_XP)) {
            RenderingExtensionDescriptor fed = (RenderingExtensionDescriptor)contribution;
            engine.unregisterRenderingExtension(fed.name);
        } else if (extensionPoint.equals(INSTALL_XP)) {
            Installer installer = (Installer)contribution;
            installer.uninstall(contributor.getContext(), engine.getRootDirectory());
        } else if (extensionPoint.equals(CONFIG_XP)) {
            ConfigurationFileDescriptor cfg = (ConfigurationFileDescriptor)contribution;
            if (cfg.path != null) {
                unloadConfiguration(new File(engine.getRootDirectory(), cfg.path));
            } else if (cfg.entry != null) {
                throw new UnsupportedOperationException("Entry is not supported for now");
            } else {
                log.error("Neither path neither entry attribute was defined in the configuration extension. Ignoring");
            }
        } else if (extensionPoint.endsWith(FORM_XP)) {
            Form form = (Form)contribution;
            engine.getFormManager().unregisterForm(form.getId());
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == WebEngine.class) {
            return adapter.cast(engine);
        } else if (adapter == FileChangeNotifier.class) {
            return adapter.cast(notifier);
        }
        return null;
    }

    public void fileChanged(FileChangeNotifier.FileEntry entry, long now) throws Exception {
        if (ctx == null) {
            return;
        }
        String path = entry.file.getAbsolutePath();
        String rootPath = engine.getRootDirectory().getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            return;
        }
        String relPath = path.substring(rootPath.length());
        if (!relPath.startsWith("/")) {
            relPath = '/' + relPath;
        }
//      if (file.getAbsolutePath().startsWith(mgr.getRootDirectory().getAbsolutePath())) {
//        if (relPath.equals("/nuxeo-web.xml")) { // TODO remove this
//            URL url = entry.file.toURI().toURL();
//            ctx.getRuntimeContext().undeploy(url);
//            ctx.getRuntimeContext().deploy(url);
//            engine.fireConfigurationChanged();
//        }
    }

    public void configurationChanged(Entry entry) throws Exception {
        if (engine != null) {
            engine.fireConfigurationChanged();
        }
    }

}
