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
 *     fox
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.model.Session.PROP_RETENTION_COMPLIANCE_MODE_ENABLED;
import static org.nuxeo.ecm.core.model.Session.PROP_RETENTION_STRICT_MODE_ENABLED;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.model.BaseSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2023.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:test-retain-files-property.xml")
public class TestRetentionStrictComplianceCompat {

    @SuppressWarnings("deprecation")
    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void testIsCompliantStrict() {
        assertTrue(BaseSession.isRetentionStricMode());
    }

    @SuppressWarnings("deprecation")
    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "false")
    public void testIsNotCompliantNotStrict() {
        assertFalse(BaseSession.isRetentionStricMode());
    }

    @SuppressWarnings("deprecation")
    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_STRICT_MODE_ENABLED, value = "false")
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "false")
    public void testIsNotCompliantNotStrictNotStrict() {
        assertFalse(BaseSession.isRetentionStricMode());
    }

    @Test
    public void testIsNotStrict() {
        assertFalse(BaseSession.isRetentionStricMode());
    }

    @SuppressWarnings("deprecation")
    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_STRICT_MODE_ENABLED, value = "true")
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "false")
    public void testIsStrictNotCompliantStrict() {
        assertTrue(BaseSession.isRetentionStricMode());
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_STRICT_MODE_ENABLED, value = "false")
    public void testIsNotStrictNotStrict() {
        assertFalse(BaseSession.isRetentionStricMode());
    }

    @SuppressWarnings("deprecation")
    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_STRICT_MODE_ENABLED, value = "false")
    @WithFrameworkProperty(name = PROP_RETENTION_COMPLIANCE_MODE_ENABLED, value = "true")
    public void testIsNotStrictCompliantNotStrict() {
        assertFalse(BaseSession.isRetentionStricMode());
    }

    @Test
    @WithFrameworkProperty(name = PROP_RETENTION_STRICT_MODE_ENABLED, value = "true")
    public void testIsStrictStrict() {
        assertTrue(BaseSession.isRetentionStricMode());
    }
}
