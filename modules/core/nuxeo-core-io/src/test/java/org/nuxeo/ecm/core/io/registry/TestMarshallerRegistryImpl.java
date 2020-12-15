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
 *     pierre
 */
package org.nuxeo.ecm.core.io.registry;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.registry.MarshallerRegistryImpl.XP_MARSHALLERS;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.io.CoreIOFeature;
import org.nuxeo.ecm.core.io.registry.TestReaderRegistry.DefaultNumberReader;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreIOFeature.class)
public class TestMarshallerRegistryImpl {

    @Inject
    private MarshallerRegistry registry;

    private final RenderingContext ctx = RenderingContext.CtxBuilder.get();

    @Test
    public void testRegistryUnregisterMarshaller() {
        registry.clear();
        assertIsEmpty();
        registry.register(DefaultNumberReader.class);
        assertHasOne();
        registry.deregister(DefaultNumberReader.class);
        assertIsEmpty();
        registry.register(DefaultNumberReader.class);
        assertHasOne();
    }

    @Test
    public void testRegisterUnregisterDescriptor() throws Exception {
        registry.clear();
        MarshallerRegistryImpl component = (MarshallerRegistryImpl) registry;
        assertIsEmpty();
        component.register(DefaultNumberReader.class);
        assertHasOne();
        component.deregister(DefaultNumberReader.class);
        assertIsEmpty();
    }

    private void assertIsEmpty() {
        assertTrue(registry.getAllReaders(ctx, Number.class, null, APPLICATION_JSON_TYPE).isEmpty());
    }

    private void assertHasOne() {
        assertEquals(1, registry.getAllReaders(ctx, Number.class, null, APPLICATION_JSON_TYPE).size());
    }

}
