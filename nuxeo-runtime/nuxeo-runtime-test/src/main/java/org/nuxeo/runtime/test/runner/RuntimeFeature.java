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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.Statement;
import org.nuxeo.common.utils.URLStreamHandlerFactoryInstaller;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.test.RuntimeHarnessImpl;
import org.nuxeo.runtime.test.runner.HotDeployer.ActionHandler;

import com.google.inject.Binder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Features({ MDCFeature.class, ConditionalIgnoreRule.Feature.class, RandomBug.Feature.class })
public class RuntimeFeature implements RunnerFeature {

    protected RuntimeHarness harness;

    protected RuntimeDeployment deployment;

    protected HotDeployer deployer;

    /**
     * Providers contributed by other features to override the default service provider used for a nuxeo service.
     */
    protected final Map<Class<?>, ServiceProvider<?>> serviceProviders;

    public RuntimeFeature() {
        serviceProviders = new HashMap<>();
    }

    public <T> void addServiceProvider(ServiceProvider<T> provider) {
        serviceProviders.put(provider.getServiceClass(), provider);
    }

    public RuntimeHarness getHarness() {
        return harness;
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        harness = new RuntimeHarnessImpl(runner.getTargetTestClass());
        deployment = RuntimeDeployment.onTest(runner);
        deployer = new HotDeployer(runner, harness);
    }

    public HotDeployer registerHandler(ActionHandler handler) {
        return deployer.addHandler(handler);
    }

    public boolean unregisterHandler(ActionHandler handler) {
        return deployer.removeHandler(handler);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(RuntimeHarness.class).toInstance(getHarness());
        binder.bind(HotDeployer.class).toInstance(deployer);
        for (String svc : Framework.getRuntime().getComponentManager().getServices()) {
            try {
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(svc);
                ServiceProvider<?> provider = serviceProviders.get(clazz);
                if (provider == null) {
                    provider = new ServiceProvider(clazz);
                }
                binder.bind(clazz).toProvider(provider).in(provider.getScope());
            } catch (Exception e) {
                throw new RuntimeException("Failed to bind service: " + svc, e);
            }
        }
    }

    @Override
    public void start(final FeaturesRunner runner) throws Exception {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                    return;
                }
                Framework.removeListener(this);
                blacklistComponents(runner);
            }
        });

        harness.start();
        deployment.deploy(runner, harness);
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        harness.stop();
    }

    @Rule
    public MethodRule onCleanupURLStreamHandlers() {
        return (base, method, target) -> new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    URLStreamHandlerFactoryInstaller.resetURLStreamHandlers();
                }
            }
        };
    }

    @Rule
    public MethodRule onMethodDeployment() {
        return RuntimeDeployment.onMethod();
    }

    protected void blacklistComponents(FeaturesRunner aRunner) {
        BlacklistComponent config = aRunner.getConfig(BlacklistComponent.class);
        if (config.value().length == 0) {
            return;
        }
        final ComponentManager manager = Framework.getRuntime().getComponentManager();
        Set<String> blacklist = new HashSet<>(manager.getBlacklist());
        blacklist.addAll(Arrays.asList(config.value()));
        manager.setBlacklist(blacklist);
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        // this will make a snapshot of the component registry and will start the components
        harness.fireFrameworkStarted();
    }

}
