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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.sql;

import org.junit.AssumptionViolatedException;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Allows to ignore all the tests from a class running this feature if the database configured for tests is not H2.
 *
 * @since 5.9.5
 */
public class H2OnlyFeature implements RunnerFeature {

    @Override
    public void start(FeaturesRunner runner) {
        if (DatabaseHelper.DATABASE instanceof DatabaseH2) {
            return;
        }
        throw new AssumptionViolatedException("Database is not H2");
    }

}
