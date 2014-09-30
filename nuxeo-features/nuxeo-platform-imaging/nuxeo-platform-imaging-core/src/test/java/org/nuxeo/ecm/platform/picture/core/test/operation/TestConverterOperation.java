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
package org.nuxeo.ecm.platform.picture.core.test.operation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.operation.ConverterOperation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.6
 *
 * @author Vincent Vergnolle
 */
@Deploy({ "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.commandline.executor" })
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
public class TestConverterOperation {

    public static final Log log = LogFactory.getLog(TestConverterOperation.class);

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    @Test
    public void iHaveTheConverterOperationRegistered() {
        Assert.assertTrue(automationService.hasOperation(ConverterOperation.ID));
    }

    @Test
    public void iCanUseTheConverterOperation() throws Exception {
        Blob blob = getTestImage();

        int height = 480;

        Map<String, Object> params = new HashMap<>(1);
        params.put("parameters", createConverterParametersMap(640, height));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blob);

        blob = (Blob) automationService.run(ctx, "Picture.Resize", params);

        ImagingService imagingService = getImagingService();
        ImageInfo imageInfo = imagingService.getImageInfo(blob);

        if (log.isDebugEnabled()) {
            log.debug("ImageInfo size: " + imageInfo.getWidth() + "*"
                    + imageInfo.getHeight());
        }

        Assert.assertEquals(height, imageInfo.getHeight());
    }

    private FileBlob getTestImage() throws IOException {
        return new FileBlob(
                TestConverterOperation.class.getResourceAsStream("/images/andy.bmp"));
    }

    private Map<String, Object> createConverterParametersMap(int width,
            int height) {
        Map<String, Object> parameters = new HashMap<>(3);
        parameters.put(ImagingConvertConstants.CONVERSION_FORMAT,
                ImagingConvertConstants.JPEG_CONVERSATION_FORMAT);
        parameters.put(ImagingConvertConstants.OPTION_RESIZE_WIDTH,
                String.valueOf(width));
        parameters.put(ImagingConvertConstants.OPTION_RESIZE_HEIGHT,
                String.valueOf(height));

        return parameters;
    }

    private ImagingService getImagingService() {
        ImagingService imagingService = Framework.getService(ImagingService.class);
        Assert.assertNotNull(imagingService);

        return imagingService;
    }
}
