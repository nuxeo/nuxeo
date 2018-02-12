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
package org.nuxeo.ecm.platform.publisher.test;

import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.platform.publisher.core.contrib")
@Deploy("org.nuxeo.ecm.platform.publisher.core")
public abstract class PublisherTestCase extends AbstractRepositoryTestCase {

}
