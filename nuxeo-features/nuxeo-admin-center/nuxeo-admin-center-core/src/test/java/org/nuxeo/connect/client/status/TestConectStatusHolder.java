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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.connect.client.status;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
import org.nuxeo.connect.identity.LogicalInstanceIdentifier.InvalidCLID;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests for {@link ConnectStatusHolder}.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(MockitoFeature.class)
public class TestConectStatusHolder {

    @Mock
    @RuntimeService
    protected ConnectRegistrationService crs;

    @Test
    public void testGetRegistrationExpirationTimestamp() throws InvalidCLID {
        // null CLID
        mockCLID(null);
        long timestamp = ConnectStatusHolder.instance().getRegistrationExpirationTimestamp();
        assertEquals(-1, timestamp);

        // old v0 format
        mockCLID("a1359ca8-eba2-4490-8b9f-c41f66d99bd5--CLID2");
        timestamp = ConnectStatusHolder.instance().getRegistrationExpirationTimestamp();
        assertEquals(-1, timestamp);

        // wrong format
        mockCLID("foo.bar--CLID2");
        timestamp = ConnectStatusHolder.instance().getRegistrationExpirationTimestamp();
        assertEquals(-1, timestamp);

        // good format, including expiration timestamp
        mockCLID("foo.1502525400.bar--CLID2");
        timestamp = ConnectStatusHolder.instance().getRegistrationExpirationTimestamp();
        assertEquals(1502525400, timestamp);
    }

    protected void mockCLID(String clid) throws InvalidCLID {
        when(crs.getCLID()).thenReturn(clid == null ? null : new LogicalInstanceIdentifier(clid));
    }

}
