/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.mongodb;

import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;

/**
 * @since 9.1
 */
public final class IgnoreNoMongoDB implements ConditionalIgnoreRule.Condition {

    // change this to force tests on a local MongoDB instance (cf MongoDBFeature for configuration)
    public static final boolean MONGODB_FORCE = false;

    // compat with what's done in StorageConfiguration
    public static final String CORE_PROPERTY = "nuxeo.test.core";

    public static final String CORE_MONGODB = "mongodb";

    @Override
    public boolean shouldIgnore() {
        if (MONGODB_FORCE) {
            return false;
        }
        if (CORE_MONGODB.equals(System.getProperty(CORE_PROPERTY))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supportsClassRule() {
        return true;
    }
}
