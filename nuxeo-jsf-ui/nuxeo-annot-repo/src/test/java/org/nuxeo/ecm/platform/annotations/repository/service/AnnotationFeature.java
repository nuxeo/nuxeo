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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.annotations")
@Deploy("org.nuxeo.ecm.annotations.contrib")
@Deploy("org.nuxeo.ecm.annotations.repository")
@Deploy("org.nuxeo.ecm.annotations.repository.test")
@Deploy("org.nuxeo.runtime.jtajca")
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.runtime.datasource:anno-ds.xml")
public class AnnotationFeature implements RunnerFeature {

    @Override
    public void initialize(FeaturesRunner runner) {
        Framework.addListener(new AnnotationsJenaSetup());
    }
}
