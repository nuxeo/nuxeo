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
 *     Nour AL KOTOB
 */

package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/test-bypass-contrib.xml")
public class TestBypassOverride {

    @Test
    public void testContribution(){
        assertTrue(ConversionServiceImpl.getConverterDescriptor("testBypassTrue").isBypassIfSameMimeType());
        assertFalse(ConversionServiceImpl.getConverterDescriptor("testBypassFalse").isBypassIfSameMimeType());
        assertFalse(ConversionServiceImpl.getConverterDescriptor("testBypassDefault").isBypassIfSameMimeType());
    }

    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/test-bypass-override-contrib.xml")
    @Test
    public void testContributionOverride(){
        assertFalse(ConversionServiceImpl.getConverterDescriptor("testBypassTrue").isBypassIfSameMimeType());
        assertTrue(ConversionServiceImpl.getConverterDescriptor("testBypassFalse").isBypassIfSameMimeType());
        assertTrue(ConversionServiceImpl.getConverterDescriptor("testBypassDefault").isBypassIfSameMimeType());
    }
}
