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
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfigs;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

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
// annotation present to provide an accessible default
public class MultiNuxeoCoreRunner extends ParentRunner<FeaturesRunner> {

    private final List<FeaturesRunner> runners = new ArrayList<FeaturesRunner>();

    private RepositorySettings[] configs;

    public MultiNuxeoCoreRunner(Class<?> testClass, RunnerBuilder builder)
            throws InitializationError {
        this(builder, testClass, getSuiteClasses(testClass),
                getRepositorySettings(testClass));
    }

    public MultiNuxeoCoreRunner(RunnerBuilder builder, Class<?> testClass,
            Class<?>[] classes, RepositorySettings[] repoTypes)
            throws InitializationError {
        this(null, builder.runners(null, classes), repoTypes);
    }

    protected MultiNuxeoCoreRunner(Class<?> klass, List<Runner> runners,
            RepositorySettings[] configs) throws InitializationError {
        super(klass);
        for (Runner runner : runners) {
            this.runners.add((FeaturesRunner) runner);
        }
        this.configs = configs;
    }

    @Override
    protected String getName() {
        return "Nuxeo Core Suite: "+getClass();
    }

    protected static RepositorySettings[] getRepositorySettings(Class<?> testClass) {
        RepositoryConfigs annotation = testClass.getAnnotation(RepositoryConfigs.class);
        if (annotation == null) {
            return new RepositorySettings[] { new RepositorySettings() };
        } else {
            RepositoryConfig[] annos = annotation.value();
            RepositorySettings[] result = new RepositorySettings[annos.length];
            for (int i=0; i<annos.length; i++) {
                result[i] = new RepositorySettings(annos[i]);
            }
            return result;
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
        return annotation.value();
    }

    @Override
    protected Description describeChild(FeaturesRunner child) {
        return child.getDescription();
    }

    @Override
    protected List<FeaturesRunner> getChildren() {
        return runners;
    }


    @Override
    protected void runChild(FeaturesRunner child, RunNotifier notifier) {
        for (RepositorySettings config : configs) {
            CoreFeature cf = child.getFeature(CoreFeature.class);
            if (cf != null) {
                cf.setRepositorySettings(config);
            }
//TODO            child.resetInjector();
            child.run(notifier);
        }
    }

}
