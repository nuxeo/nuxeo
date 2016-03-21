/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Tests from the SQLDirectoryTestSuite but with directory configuration defined through a template indirection.
 *
 * @since 8.2
 */
@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@LocalDeploy({ //
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-from-template.xml", //
})
public class TestSQLDirectoryTemplate extends SQLDirectoryTestSuite {

}
