/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.runtime.test.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;

/**
 * @since 5.8
 */

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class CanMockServicesTest {

    @RuntimeService
    @Mock
    AFakeService myService;

    @Before
    public void doBefore() {
        when(myService.getSomething()).thenReturn("Hello !");
    }

    @Test
    public void itShouldBindMocktoAService() throws Exception {
        AFakeService service = Framework.getService(AFakeService.class);
        assertNotNull(service);
        assertEquals("Hello !", service.getSomething());
    }

    @Test
    public void itShouldMockFields() throws Exception {
        assertEquals("Hello !", myService.getSomething());
    }

}
