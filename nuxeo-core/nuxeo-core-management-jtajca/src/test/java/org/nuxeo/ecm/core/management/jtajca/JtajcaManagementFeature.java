/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.management.jtajca;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.management.ManagementFeature;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer.ActionHandler;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

// ideally this feature should deploy/use TransactionalFeature but we can't depend on it because features order is
// important as feature is doing assertion. So we need to keep this order to ensure assertion is at good time:
// - CoreFeature (close and clean session - need a transaction)
// - TransactionalFeature (close the transaction)
// - JtajcaManagementFeature (assert there's no remaining transactions)
// we get this order by using features in tests like this @Features(JtajcaManagementFeature.class, CoreFeature.class)
@Features(ManagementFeature.class)
@Deploy("org.nuxeo.ecm.core.management.jtajca")
@Deploy("org.nuxeo.ecm.core.management.jtajca:login-config.xml")
public class JtajcaManagementFeature extends SimpleFeature {

    protected static ObjectName nameOf(Class<?> itf) {
        try {
            return new ObjectName(Defaults.instance.name(itf, "*"));
        } catch (MalformedObjectNameException cause) {
            throw new AssertionError("Cannot name monitor", cause);
        }
    }

    protected <T> void bind(Binder binder, MBeanServer mbs, Class<T> type) {
        final Set<ObjectName> names = mbs.queryNames(nameOf(type), null);
        for (ObjectName name : names) {
            binder.bind(type)
                  .annotatedWith(Names.named(name.getKeyProperty("name")))
                  .toProvider(new MBeanProvider<>(type, name));
        }
    }

    class MBeanProvider<T> implements Provider<T> {
        protected Class<T> type;

        protected ObjectName name;

        public MBeanProvider(Class<T> type, ObjectName name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public T get() {
            MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
            return type.cast(JMX.newMXBeanProxy(mbs, name, type));
        }
    }

    public static <T> T getInstanceNamedWithPrefix(Class<T> type, String prefix) {
        MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
        Set<String> names = new HashSet<>();
        for (ObjectName objectName : mbs.queryNames(nameOf(type), null)) {
            String name = objectName.getKeyProperty("name");
            names.add(name); // for error case
            if (name.startsWith(prefix)) {
                return JMX.newMXBeanProxy(mbs, objectName, type);
            }
        }
        throw new RuntimeException("Found no bean with name prefix: " + prefix + " in available names: " + names);
    }

    CoreFeature core;

    Class<?> target;

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        core = runner.getFeature(CoreFeature.class);
        target = runner.getTargetTestClass();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        if (core == null) {
            return;
        }
        runner.getFeature(RuntimeFeature.class).registerHandler(new JtajcaDeployer(runner));
        // bind repository
        String repositoryName = core.getStorageConfiguration().getRepositoryName();
        NuxeoContainer.getConnectionManager(repositoryName);

        MBeanServer mbs = Framework.getService(ServerLocator.class).lookupServer();
        bind(binder, mbs, ConnectionPoolMonitor.class);
        bind(binder, mbs, CoreSessionMonitor.class);
        TransactionManager tm = NuxeoContainer.getTransactionManager();
        if (tm != null) {
            bind(binder, mbs, TransactionMonitor.class);
            binder.bind(TransactionManager.class).toInstance(tm);
        }
    }

    class TxChecker {
        @Inject
        @Named("default")
        TransactionMonitor monitor;

        void assertNoTransactions() {
            long count = monitor.getActiveCount();
            if (count == 0) {
                LogFactory.getLog(JtajcaManagementFeature.class).debug(target + " was successful");
                return;
            }
            throw new AssertionError(
                    String.format("still have tx active (%d) %s", count, monitor.getActiveStatistics()));
        }

        public TxChecker(FeaturesRunner runner) {
            runner.getInjector().injectMembers(this);
        }
    }

    TxChecker txChecker;

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        if (core == null) {
            return;
        }
        txChecker = new TxChecker(runner);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        if (txChecker == null) {
            return;
        }
        try {
            txChecker.assertNoTransactions();
        } finally {
            txChecker = null;
        }
    }

    public class JtajcaDeployer extends ActionHandler {

        protected final FeaturesRunner runner;

        public JtajcaDeployer(FeaturesRunner runner) {
            this.runner = runner;
        }

        @Override
        public void exec(String action, String... args) throws Exception {
            // if components are restarted (due to a hot deploy) while in a test method we need to register
            // a deploy handler to recreate the tx checker.
            next.exec(action, args);
            txChecker = new TxChecker(runner);
        }
    }

}
