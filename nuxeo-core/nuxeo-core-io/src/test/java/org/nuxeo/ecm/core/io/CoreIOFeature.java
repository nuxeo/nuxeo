/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.io;

import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Intermediate feature for nuxeo-core-io module.
 *
 * @since 10.2
 */
@Deploy("org.nuxeo.runtime.stream:OSGI-INF/avro-service.xml") // just deploy Avro
@Deploy("org.nuxeo.runtime.stream:OSGI-INF/avro-contrib.xml") // just deploy Avro
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.io")
@Features(RuntimeFeature.class)
public class CoreIOFeature extends SimpleFeature {

}
