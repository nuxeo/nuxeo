/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.mongodb.blob;

import org.nuxeo.ecm.core.blob.TestAbstractBlobStore;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2023.5
 */
@Features(MongoDBFeature.class)
@Deploy("org.nuxeo.ecm.core.api")
@Deploy("org.nuxeo.ecm.core.storage")
@Deploy("org.nuxeo.ecm.core.storage.mongodb.test:OSGI-INF/test-gridfs-config.xml")
@WithFrameworkProperty(name = "nuxeo.core.binarymanager", value = "org.nuxeo.ecm.core.storage.mongodb.blob.GridFSBlobProvider")
public class TestGridFSBlobStore extends TestAbstractBlobStore {

}
