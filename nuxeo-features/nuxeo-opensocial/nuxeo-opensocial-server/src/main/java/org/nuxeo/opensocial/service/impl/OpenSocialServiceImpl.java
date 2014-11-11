/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.shindig.crypto.KeyDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

import com.google.inject.Injector;

public class OpenSocialServiceImpl extends DefaultComponent implements
        OpenSocialService {
    public static final String ID = "org.nuxeo.opensocial.service.impl.OpenSocialServiceImpl";

    public static final ComponentName NAME = new ComponentName(ID);

    private static final Log LOG = LogFactory.getLog(OpenSocialService.class);

    private static final String XP_CRYPTO = "cryptoConfig";

    private static Injector injector;

    private Map<String, String> keys = new HashMap<String, String>();

    private static final String SHINDIG_PROXY_PROXY_PORT = "shindig.proxy.proxyPort";
    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";
    private static final String SHINDIG_PROXY_PROXY_SET = "shindig.proxy.proxySet";
    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";
    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    private Proxy proxySettings = null;

    public Injector getInjector() {
        return injector;
    }

    public GadgetSpecFactory getGadgetSpecFactory() {
        return injector.getInstance(GadgetSpecFactory.class);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_CRYPTO.equals(extensionPoint)) {
            KeyDescriptor kd = (KeyDescriptor) contribution;
            keys.put(kd.getContainer(), kd.getKey());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (XP_CRYPTO.equals(extensionPoint)) {
            KeyDescriptor kd = (KeyDescriptor) contribution;
            if (keys.containsKey(kd.getContainer())) {
                keys.remove(kd.getContainer());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(this.getClass())) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        LOG.info("Activate component OpenSocial service");

        File root = new File(Framework.getRuntime().getHome(), "opensocial");

        // Be sure to delete the deployment root before deploying
        if (root.exists()) {
            FileUtils.deleteTree(root);
        }

        try {
            root = root.getCanonicalFile();
            LOG.info("Using web root: " + root);
            if (!new File(root, "WEB-INF").exists()) {
                try {
                    root.mkdirs();
                    // runtime predeployment is not supporting conditional
                    // unziping so we
                    // do
                    // the predeployment here:
                    deployWebDir(context.getRuntimeContext().getBundle(), root);
                } catch (Exception e) { // delete incomplete files
                    FileUtils.deleteTree(root);
                    throw e;
                }
            }

        } catch (Exception e1) {
            LOG.error("Unable to deploy opensocial web dir");
        }

    }

    public static void copyResources(Bundle bundle, String path, File root)
            throws IOException {
        File file = Framework.getRuntime().getBundleFile(bundle);
        if (file == null) {
            throw new UnsupportedOperationException(
                    "Couldn't transform the bundle location into a file");
        }
        root.mkdirs();
        if (file.isDirectory()) {
            file = new File(file, path);
            FileUtils.copy(file.listFiles(), root);
        } else {
            ZipUtils.unzip(path, file, root);
        }
    }

    private static void deployWebDir(Bundle bundle, File root)
            throws IOException {
        copyResources(bundle, "opensocial", root);
    }

    @Override
    public void deactivate(ComponentContext arg0) {
        LOG.info("DeActivate component OpenSocial service");
    }

    public Object getInstance(Class<?> klass) {
        if (getInjector() != null)
            return getInjector().getInstance(klass);
        else
            return null;
    }

    public void setInjector(Injector injector) {
        OpenSocialServiceImpl.injector = injector;
    }

    public String getKeyForContainer(String defaultContainer) {
        return keys.get(defaultContainer);
    }

    public Proxy getProxySettings() {
        if (isProxySet()) {
            if (proxySettings == null) {
                setAuthenticator();
                proxySettings = new Proxy(
                        Proxy.Type.HTTP,
                        new InetSocketAddress(Framework
                                .getProperty(SHINDIG_PROXY_PROXY_HOST), Integer
                                .parseInt(Framework
                                        .getProperty(SHINDIG_PROXY_PROXY_PORT))));
            }

            return proxySettings;
        } else {
            return Proxy.NO_PROXY;
        }
    }

    private static void setAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {

                String password = Framework.getProperty(SHINDIG_PROXY_PASSWORD);
                if (password != null) {
                    return new PasswordAuthentication(Framework
                            .getProperty(SHINDIG_PROXY_USER), password
                            .toCharArray());
                }
                return null;

            }
        });
    }

    private static boolean isProxySet() {
        return Framework.getProperty(SHINDIG_PROXY_PROXY_SET) != null
                && Framework.getProperty(SHINDIG_PROXY_PROXY_SET)
                        .equals("true");
    }

}
