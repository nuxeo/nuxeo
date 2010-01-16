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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryBackends;

/**
 * JUnit4 ParentRunner that knows how to run a test class on multiple backend
 * types.
 * <p>
 * To use it :
 *
 * <pre>
 * &#064;RunWith(MultiNuxeoCoreRunner.class)
 * &#064;SuiteClasses(SimpleSession.class)
 * &#064;Repositories( { RepoType.H2, RepoType.JCR, RepoType.POSTGRES })
 * public class NuxeoSuiteTest {
 * }
 * </pre>
 *
 * With SimpleSession.class being a class to be run with NuxeoCoreRunner
 */
@RepositoryBackends
// annotation present to provide an accessible default
public class MultiNuxeoCoreRunner extends ParentRunner<NuxeoCoreRunner> {

    private final List<NuxeoCoreRunner> runners = new ArrayList<NuxeoCoreRunner>();

    private BackendType[] types;

    public MultiNuxeoCoreRunner(Class<?> testClass, RunnerBuilder builder)
            throws InitializationError {
        this(builder, testClass, getSuiteClasses(testClass),
                getBackendTypes(testClass));
    }

    public MultiNuxeoCoreRunner(RunnerBuilder builder, Class<?> testClass,
            Class<?>[] classes, BackendType[] repoTypes)
            throws InitializationError {
        this(null, builder.runners(null, classes), repoTypes);
    }

    protected MultiNuxeoCoreRunner(Class<?> klass, List<Runner> runners,
            BackendType[] types) throws InitializationError {
        super(klass);
        for (Runner runner : runners) {
            this.runners.add((NuxeoCoreRunner) runner);
        }
        this.types = types;
    }

    protected static BackendType[] getBackendTypes(Class<?> testClass) {
        RepositoryBackends annotation = testClass.getAnnotation(RepositoryBackends.class);
        if (annotation == null) {
            return MultiNuxeoCoreRunner.class.getAnnotation(RepositoryBackends.class).value();
        } else {
            return annotation.value();
        }
    }

    protected static Class<?>[] getSuiteClasses(Class<?> klass)
            throws InitializationError {
        SuiteClasses annotation = klass.getAnnotation(SuiteClasses.class);
        if (annotation == null) {
            throw new InitializationError(String.format(
                    "class '%s' must have a SuiteClasses annotation",
                    klass.getName()));
        }
        for (Class<?> testClass : annotation.value()) {
            if (!testClass.getAnnotation(RunWith.class).value().isAssignableFrom(
                    NuxeoCoreRunner.class)) {
                throw new InitializationError(String.format(
                        "class '%s' must be RunWith a NuxeoCoreRunner",
                        klass.getName()));
            }
        }
        return annotation.value();
    }

    @Override
    protected Description describeChild(NuxeoCoreRunner child) {
        return child.getDescription();
    }

    @Override
    protected List<NuxeoCoreRunner> getChildren() {
        return runners;
    }

    @Override
    protected void runChild(NuxeoCoreRunner child, RunNotifier notifier) {
        for (BackendType type : types) {
            child.setBackendType(type);
            child.resetInjector();
            child.run(notifier);
        }
    }

}
