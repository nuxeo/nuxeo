/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.convert.test.operation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.platform.picture.convert.operation.ConverterOperation;
import org.nuxeo.ecm.platform.picture.convert.test.NopConverter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @since 7.1
 *
 * @author Vincent Vergnolle
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.commandline.executor" })
public class TestConverterOperation {

    private static final String TEST_CONVERTER_CONTRIB = "OSGI-INF/test-converter-contrib.xml";

    private static final String PICTURE_CONVERT_BUNDLE = "org.nuxeo.ecm.platform.picture.convert";

    public static final Log log = LogFactory.getLog(TestConverterOperation.class);

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;

    @Inject
    protected RuntimeHarness harness;

    @Test
    public void iHaveTheConverterOperationRegistered() {
        Assert.assertTrue(automationService.hasOperation(ConverterOperation.ID));
    }

    @Test
    public void iCanUseTheConverterOperation() throws Exception {
        harness.deployContrib(PICTURE_CONVERT_BUNDLE, TEST_CONVERTER_CONTRIB);

        int height = 480;
        Blob blob = getTestImage();

        Map<String, Object> params = new HashMap<>();
        params.put("converter", NopConverter.ID);
        params.put("parameters", createConverterParameters(640, height));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);

        Blob resultBlob = (Blob) automationService.run(ctx,
                ConverterOperation.ID, params);

        Assert.assertTrue(blob.equals(resultBlob));
        harness.undeployContrib(PICTURE_CONVERT_BUNDLE, TEST_CONVERTER_CONTRIB);
    }

    protected FileBlob getTestImage() throws IOException {
        return new FileBlob(
                TestConverterOperation.class.getResourceAsStream("/test-data/big_nuxeo_logo.gif"));
    }

    protected Object createConverterParameters(int width, int height) {
        Properties parameters = new Properties();
        parameters.put(ImagingConvertConstants.CONVERSION_FORMAT,
                ImagingConvertConstants.JPEG_CONVERSATION_FORMAT);
        parameters.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH,
                String.valueOf(width));
        parameters.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT,
                String.valueOf(height));

        return parameters;
    }
}
