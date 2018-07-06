/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     slacoin
 */
package org.nuxeo.ecm.core.test;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

public class DetectThreadDeadlocksFeature implements RunnerFeature {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Config {

        boolean dumpAtTearDown() default false;

        boolean dumpOnFailure() default true;
    }

    @Inject
    protected RunNotifier notifier;

    protected ThreadDeadlocksDetector detector = new ThreadDeadlocksDetector();

    protected Config config;

    protected final RunListener listener = new RunListener() {
        @Override
        public void testFailure(Failure failure) throws Exception {
            dump();
        }
    };

    @Override
    public void initialize(FeaturesRunner runner) {
        config = runner.getConfig(Config.class);
    }

    @Override
    public void beforeRun(FeaturesRunner runner) {
        runner.getInjector().injectMembers(this);
        if (config.dumpOnFailure()) {
            notifier.addListener(listener);
        }
        detector.schedule(30 * 1000, new ThreadDeadlocksDetector.KillListener());
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (config.dumpOnFailure()) {
            notifier.removeListener(listener);
        }
        if (config.dumpAtTearDown()) {
            dump();
        }
        detector.cancel();
    }

    protected void dump() throws IOException {
        long[] detectThreadLock = detector.detectThreadLock();
        File dump = detector.dump(detectThreadLock);
        LogFactory.getLog(DetectThreadDeadlocksFeature.class).warn("Thread dump available at " + dump);
    }
}
