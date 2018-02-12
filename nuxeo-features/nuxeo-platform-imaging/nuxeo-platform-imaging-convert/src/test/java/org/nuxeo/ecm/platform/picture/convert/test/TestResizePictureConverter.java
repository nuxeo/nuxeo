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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.convert.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.1
 * @author Vincent Vergnolle
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.picture.convert")
public class TestResizePictureConverter {

    @Test
    public void iShouldHaveResizePictureConverterRegistered() {
        ConverterCheckResult check = Framework.getService(ConversionService.class).isConverterAvailable(
                ImagingConvertConstants.OPERATION_RESIZE);
        Assert.assertTrue(check.isAvailable());
    }

}
