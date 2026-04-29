/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core;

import static org.nuxeo.ecm.core.test.StorageConfiguration.CORE_MEM;
import static org.nuxeo.ecm.core.test.StorageConfiguration.CORE_MONGODB;
import static org.nuxeo.ecm.core.test.StorageConfiguration.CORE_PROPERTY;
import static org.nuxeo.ecm.core.test.StorageConfiguration.DEFAULT_CORE;

import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

/**
 * Condition to ignore a test when not running on a DBS repository (MongoDB or mem).
 *
 * @since 2025.19
 */
public class IgnoreIfNotDBSRepository implements ConditionalIgnoreRule.Condition {

    @Override
    public boolean shouldIgnore() {
        String coreType = System.getProperty(CORE_PROPERTY, DEFAULT_CORE);
        return !(CORE_MEM.equals(coreType) || CORE_MONGODB.equals(coreType));
    }
}
