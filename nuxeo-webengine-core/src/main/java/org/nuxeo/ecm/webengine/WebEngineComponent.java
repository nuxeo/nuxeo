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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.install.Installer;
import org.nuxeo.ecm.webengine.rendering.RenderingTemplateDescriptor;
import org.nuxeo.ecm.webengine.rendering.TransformerDescriptor;
import org.nuxeo.ecm.webengine.security.GuardDescriptor;
import org.nuxeo.ecm.webengine.security.PermissionService;
import org.nuxeo.ecm.webengine.util.FileChangeListener;
import org.nuxeo.ecm.webengine.util.FileChangeNotifier;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineComponent extends DefaultComponent implements FileChangeListener {

    public static final ComponentName NAME = new ComponentName(WebEngineComponent.class.getName());

    public static final String TRANSFORMER_XP = "transformer";
    public static final String WEB_OBJ_XP = "webObject";
    public static final String BINDING_XP = "binding";
    public static final String GUARD_XP = "guard"; // global guards
    public final static String RENDERING_TEMPLATE_XP = "rendering-template";
    public final static String APPLICATION_XP = "application";
    public final static String INSTALL_XP = "install";
    public final static String CONFIG_XP = "configuration";

    private static final Log log = LogFactory.getLog(WebEngineComponent.class);

    private WebEngine engine;
    private FileChangeNotifier notifier;
    private ComponentContext ctx;

    private ResourceBundle messages;

    private WebApplicationDescriptorRegistry appReg;

    @Override
    public void activate(ComponentContext context) throws Exception {
        ctx = context;
        appReg = new WebApplicationDescriptorRegistry();
        File root = new File(Framework.getRuntime().getHome(), "web");
        root = root.getCanonicalFile();
        if (!root.exists()) {
            root.mkdirs();
            // runtime predeployment is not supporting conditional unziping so we do the predeployment here:
            deployWebDir(context.getRuntimeContext().getBundle(), root);
        }
        engine = new DefaultWebEngine(root, messages);
        notifier = new FileChangeNotifier();
        notifier.start();
        notifier.addListener(this);

        // load message bundle
        loadMessageBundle(root, false);

        // load configuration (it ill be put in pending until this component will exit activation code)
        File file = new File(root, "nuxeo-web.xml");
        if (file.isFile()) {
            //XMap xmap = new XMap();
            //WebConfiguration cfg = xmap.load(new XMapContext(context.getRuntimeContext()), new BufferedInputStream(new FileInputStream(file)));
            //new XMapContext(context.getRuntimeContext());
            context.getRuntimeContext().deploy(file.toURI().toURL());
            notifier.watch(file);
        }
    }

    private void loadMessageBundle(File root, boolean reload) throws IOException {
        File file = new File(root, "i18n");
        WebClassLoader cl = new WebClassLoader();
        cl.addFile(file);
        messages = ResourceBundle.getBundle("messages", Locale.getDefault(), cl);
        engine.setMessages(messages);
        if (!reload) {
            notifier.watch(file);
            for (File f : file.listFiles()) {
                notifier.watch(f);
            }
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        notifier.stop();
        notifier.removeListener(this);
        appReg = null;
        notifier = null;
        ctx = null;
    }

    private static void deployWebDir(Bundle bundle, File root) throws URISyntaxException, IOException {
        Installer.copyResources(bundle, "web", root);
    }

    public void loadConfiguration(RuntimeContext context, File file, boolean trackChanges) throws Exception {
        try {
            context.deploy(file.toURI().toURL());
        } finally {
            if (trackChanges) {
                notifier.watch(file);
            }
            if (engine != null) {
                engine.fireConfigurationChanged();
            }
        }
    }

    public void unloadConfiguration(RuntimeContext context, File file, boolean trackChanges) throws Exception {
        context.undeploy(file.toURI().toURL());
        if (trackChanges) {
            notifier.unwatch(file);
        }
        if (engine != null) {
            engine.fireConfigurationChanged();
        }
    }

    public void unregisterApplication(WebApplicationDescriptor desc) throws WebException {
        desc = appReg.remove(desc);
        if (desc == null) {
            engine.unregisterApplication(desc.id);
        } else {
            engine.registerApplication(desc);
        }
        engine.fireConfigurationChanged();
    }

    public void registerApplication(WebApplicationDescriptor desc) throws WebException {
        desc = appReg.add(desc);
        if (desc != null) {
            engine.registerApplication(desc);
        }
        engine.fireConfigurationChanged();
    }


    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (TRANSFORMER_XP.equals(extensionPoint)) {
            TransformerDescriptor td = (TransformerDescriptor)contribution;
            engine.registerRenderingTransformer(td.getName(), td.newInstance());
        } else if (WEB_OBJ_XP.equals(extensionPoint)) {
            ObjectDescriptor obj = (ObjectDescriptor)contribution;
            engine.registerObject(obj);
        } else if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().registerGuard(gd.getId(), gd.getGuard());
        } else if (BINDING_XP.equals(extensionPoint)) {
            ObjectBindingDescriptor binding = (ObjectBindingDescriptor)contribution;
            engine.registerBinding(binding.type, binding.objectId);
        } else if (extensionPoint.equals(RENDERING_TEMPLATE_XP)) {
            RenderingTemplateDescriptor fed = (RenderingTemplateDescriptor)contribution;
            try {
                engine.registerRenderingTemplate(fed.name, fed.newInstance());
            } catch (Exception e) {
                throw new RuntimeServiceException("Deployment Error. Failed to contribute freemarker template extension: "+fed.name);
            }
        } else if (extensionPoint.equals(APPLICATION_XP)) {
            WebApplicationDescriptor desc = (WebApplicationDescriptor)contribution;
            registerApplication(desc);
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
        }
    }


    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (TRANSFORMER_XP.equals(extensionPoint)) {
            TransformerDescriptor td = (TransformerDescriptor)contribution;
            engine.unregisterRenderingTransformer(td.getName());
        } else if (WEB_OBJ_XP.equals(extensionPoint)) {
            ObjectDescriptor obj = (ObjectDescriptor)contribution;
            engine.unregisterObject(obj);
        } else if (GUARD_XP.equals(extensionPoint)) {
            GuardDescriptor gd = (GuardDescriptor)contribution;
            PermissionService.getInstance().unregisterGuard(gd.getId());
        } else if (BINDING_XP.equals(extensionPoint)) {
            ObjectBindingDescriptor binding = (ObjectBindingDescriptor)contribution;
            engine.unregisterBinding(binding.type);
        } else if (extensionPoint.equals(RENDERING_TEMPLATE_XP)) {
            RenderingTemplateDescriptor fed = (RenderingTemplateDescriptor)contribution;
            engine.unregisterRenderingTemplate(fed.name);
        } else if (extensionPoint.equals(APPLICATION_XP)) {
            WebApplicationDescriptor desc = (WebApplicationDescriptor)contribution;
            unregisterApplication(desc);
        } else if (extensionPoint.equals(INSTALL_XP)) {
            Installer installer = (Installer)contribution;
            installer.uninstall(contributor.getContext(), engine.getRootDirectory());
        } else if (extensionPoint.equals(CONFIG_XP)) {
            ConfigurationFileDescriptor cfg = (ConfigurationFileDescriptor)contribution;
            if (cfg.path != null) {
                unloadConfiguration(contributor.getContext(), new File(engine.getRootDirectory(), cfg.path), cfg.trackChanges);
            } else if (cfg.entry != null) {
                throw new UnsupportedOperationException("Entry is not supported for now");
            } else {
                log.error("Neither path neither entry attribute was defined in the configuration extension. Ignoring");
            }
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

    public void fileChanged(File file, long since, long now) {
        if (ctx == null) {
            return;
        }
        String path = file.getAbsolutePath();
        String rootPath = engine.getRootDirectory().getAbsolutePath();
        if (!path.startsWith(rootPath)) {
            return;
        }
        String relPath = path.substring(rootPath.length());
        if (!relPath.startsWith("/")) {
            relPath = '/' + relPath;
        }
//        if (file.getAbsolutePath().startsWith(mgr.getRootDirectory().getAbsolutePath())) {
        if (relPath.endsWith("nuxeo-web.xml")) {
            try {
                engine.reset();
                URL url = file.toURI().toURL();
                ctx.getRuntimeContext().undeploy(url);
                ctx.getRuntimeContext().deploy(url);
                engine.fireConfigurationChanged();
            } catch (Exception e) {
                log.error("Failed to redeploy nuxeo-web.xml", e);
            }
        } else if (relPath.startsWith("/i18n/")) { // reload message bundle
            try {
                loadMessageBundle(engine.getRootDirectory(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
