/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.mockito.configuration.RuntimeService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.8
 */

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
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
