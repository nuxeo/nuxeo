/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

public class FulltextSearchDisabledFeature implements RunnerFeature {
    private static final String KEY = "nuxeo.test.fulltext.search.disabled";

    private String flag;

    @Override
    public void initialize(FeaturesRunner runner) {
        flag = System.setProperty(KEY, "true");
    }

    @Override
    public void stop(FeaturesRunner runner) {
        if (flag == null) {
            System.clearProperty(KEY);
        } else {
            System.setProperty(KEY, flag);
        }
    }

}
