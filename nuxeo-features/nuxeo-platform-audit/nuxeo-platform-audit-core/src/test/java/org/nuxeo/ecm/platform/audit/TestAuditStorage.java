/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditStorage extends AbstractAuditStorageTest {

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testStartsWith() throws Exception {
        super.testStartsWith();

        // A partial match is supported by the database query
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/eve");
        assertStartsWithCount(NUM_OF_EVENTS / 2, "/is/od");
    }

    @Override
    protected void flush() throws Exception {
        txFeature.nextTransaction();
    }
}
