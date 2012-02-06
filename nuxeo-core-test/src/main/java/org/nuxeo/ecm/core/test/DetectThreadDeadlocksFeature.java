/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class DetectThreadDeadlocksFeature extends SimpleFeature {

    protected ThreadDeadlocksDetector detector = new ThreadDeadlocksDetector();

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        detector.schedule(30 * 1000, new ThreadDeadlocksDetector.KillListener());
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        detector.cancel();
    }
}
