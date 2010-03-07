/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime.test.runner;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(RuntimeFeature.class);

    protected RuntimeHarness harness;

    protected DeploymentSet deploy;

    /**
     * Providers contributed by other features to override the default service provider
     * used for a nuxeo service.
     */
    protected Map<Class<?>, Provider<?>> serviceProviders;

    public RuntimeFeature() {
        harness = new NXRuntimeTestCase();
        deploy = new DeploymentSet();
        serviceProviders = new HashMap<Class<?>, Provider<?>>();
    }

    public <T> void addServiceProvider(Class<T> clazz, Provider<T> provider) {
        serviceProviders.put(clazz, provider);
    }

    public RuntimeHarness getHarness() {
        return harness;
    }

    public DeploymentSet deployments() {
        return deploy;
    }

    private void scanDeployments(FeaturesRunner runner) {
        List<RunnerFeature> features = runner.getFeatures();
        if (features == null) {
            throw new IllegalStateException("Cannot call scanDeployments until features are not loaded");
        }
        for (RunnerFeature feature : features) {
            deploy.load(FeaturesRunner.getScanner(), feature.getClass());
        }
        // load deployments from class to run
        deploy.load(FeaturesRunner.getScanner(), runner.getTestClass().getJavaClass());
    }


    public String[] getDeployments() {
        return deploy.getDeployments().toArray(new String[deploy.getDeployments().size()]);
    }

    public String[] getLocalDeployments() {
        return deploy.getLocalDeployments().toArray(new String[deploy.getLocalDeployments().size()]);
    }

    /**
     * Deploys bundles specified in the @Bundles annotation.
     */
    protected void deployTestClassBundles() throws Exception {
        String[] bundles = getDeployments();
        if (bundles.length > 0) {
            harness = getHarness();
            for (String bundle : bundles) {
                try {
                    int p = bundle.indexOf(':');
                    if (p == -1) {
                        harness.deployBundle(bundle);
                    } else {
                        harness.deployContrib(bundle.substring(0, p), bundle.substring(p+1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Unable to deploy artifact: " + bundle, e);
                }
            }
        }
        String[] localResources = getLocalDeployments();
        if (localResources.length > 0) {
            harness = getHarness();
            for (String bundle : localResources) {
                try {
                    int p = bundle.indexOf(':');
                    if (p == -1) {
                        throw new IllegalArgumentException("Local resources must specify a traget bundle. "+bundle);
                    } else {
                        URL url = getClass().getClassLoader().getResource(bundle.substring(p+1));
                        harness.deployTestContrib(bundle.substring(0, p), url);
                    }
                } catch (Exception e) {
                    log.error("Unable to deploy artifact: " + bundle, e);
                }
            }
        }
        harness.fireFrameworkStarted();
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        scanDeployments(runner);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // Starts Nuxeo Runtime
        if (!harness.isStarted()) {
            harness.start();
        }
        // Deploy bundles
        deployTestClassBundles();
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        // Stops the harness if needed
        if (harness.isStarted()) {
            harness.stop();
            //harness = null;
        }
    }

    //TODO this is not ok. we should not force 2 modules layers - we should be able to load any number of module layers.
    @SuppressWarnings("unchecked")
    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        for (String svc : Framework.getRuntime().getComponentManager().getServices()) {
            try {
                Class clazz = Class.forName(svc);
                Provider provider = serviceProviders.get(clazz);
                if (provider == null) {
                    bind0(binder, clazz);
                } else {
                    bind0(binder, clazz, provider);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to bind service: "+svc, e);
            }
        }
        binder.bind(RuntimeHarness.class).toInstance(getHarness());
//        binder.bind(FeaturesRunner.class).toInstance(runner);
//        binder.bind(NuxeoRunner.class).toInstance(runner);
    }


    protected <T> void bind0(Binder binder, Class<T> type) {
        binder.bind(type).toProvider(new ServiceProvider<T>(type)).in(Scopes.SINGLETON);
    }

    protected <T> void bind0(Binder binder, Class<T> type, Provider<T> provider) {
        binder.bind(type).toProvider(provider).in(Scopes.SINGLETON);
    }

    public static void bindDatasource(String key, DataSource ds) throws Exception {
        InitialContext initialCtx = new InitialContext();
        JndiHelper.rebind(initialCtx, DataSourceHelper.getDataSourceJNDIName(key), ds);
    }

}
