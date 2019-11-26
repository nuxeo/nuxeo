/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nxueo.ecm.core.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.scroll.StaticScrollRequest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestScrollService {

    @Inject
    protected ScrollService service;

    @Test
    public void testScrollService() {
        assertNotNull(service);
        assertTrue(service.exists("default"));
        assertTrue(service.exists("static"));
        assertTrue(service.exists("repository"));
        assertFalse(service.exists("unknown"));
    }

    @Test
    public void testStaticScroll() {
        String ids = "first,2,3,4,5,6,7,8,9,last";
        int scrollSize = 4;
        ScrollRequest request = new StaticScrollRequest.Builder(ids).scrollSize(scrollSize).build();
        Scroll scroll = service.scroll(request);
        assertNotNull(scroll);
        assertTrue(scroll.fetch());

        assertEquals(scrollSize, scroll.getIds().size());
        assertEquals(Arrays.asList("first", "2", "3", "4"), scroll.getIds());

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("5", "6", "7", "8"), scroll.getIds());

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("9", "last"), scroll.getIds());

        assertFalse(scroll.fetch());
        assertEquals(Collections.emptyList(), scroll.getIds());
    }

}
