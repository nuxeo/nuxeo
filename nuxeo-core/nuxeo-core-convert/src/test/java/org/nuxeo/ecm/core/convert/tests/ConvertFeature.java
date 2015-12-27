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
 *     ataillefer
 */
package org.nuxeo.ecm.core.convert.tests;

import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 6.0
 */
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.core.api", "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert" })
public class ConvertFeature extends SimpleFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // do testing configuration
        // cachesize = -1 (actually 0)
        // GC interval negative => interpreted as seconds
        ConversionServiceImpl.setMaxCacheSizeInKB(-1);
        ConversionServiceImpl.setGCIntervalInMinutes(-1000);
    }
}
