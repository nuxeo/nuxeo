/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public class VideoServiceImpl extends DefaultComponent implements VideoService {

    private static final Log log = LogFactory.getLog(VideoServiceImpl.class);

    public static final String VIDEO_CONVERSIONS_EP = "videoConversions";

    public static final String DEFAULT_VIDEO_CONVERSIONS_EP = "automaticVideoConversions";

    private VideoConversionContributionHandler videoConversions;

    private AutomaticVideoConversionContributionHandler automaticVideoConversions;

    private BlockingQueue<Runnable> conversionTaskQueue;

    private ThreadPoolExecutor conversionExecutor;

    @Override
    public void activate(ComponentContext context) throws Exception {
        videoConversions = new VideoConversionContributionHandler();
        automaticVideoConversions = new AutomaticVideoConversionContributionHandler();

        AsyncEventExecutor.NamedThreadFactory serializationThreadFactory = new AsyncEventExecutor.NamedThreadFactory(
                "Nuxeo Async Video Conversion");
        conversionTaskQueue = new LinkedBlockingQueue<Runnable>();
        conversionExecutor = new ThreadPoolExecutor(1, 1, 5, TimeUnit.MINUTES,
                conversionTaskQueue, serializationThreadFactory);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        conversionTaskQueue.clear();
        conversionExecutor.shutdownNow();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            videoConversions.addContribution((VideoConversion) contribution);
        } else if (DEFAULT_VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            automaticVideoConversions.addContribution((AutomaticVideoConversion) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            videoConversions.removeContribution((VideoConversion) contribution);
        } else if (DEFAULT_VIDEO_CONVERSIONS_EP.equals(extensionPoint)) {
            automaticVideoConversions.removeContribution((AutomaticVideoConversion) contribution);
        }
    }

    @Override
    public void launchConversion(DocumentModel doc, String conversionName) {
        VideoConversionTask task = new VideoConversionTask(doc, conversionName,
                this);
        conversionExecutor.execute(task);
    }

    @Override
    public void launchAutomaticConversions(DocumentModel doc) {
        for (AutomaticVideoConversion conversion : automaticVideoConversions.registry.values()) {
            launchConversion(doc, conversion.getName());
        }
    }

    @Override
    public Blob convert(Blob originalVideo, String conversionName)
            throws ClientException {
        if (!videoConversions.registry.containsKey(conversionName)) {
            throw new ClientRuntimeException(String.format(
                    "'%s' is not a registered video conversion.",
                    conversionName));
        }

        BlobHolder blobHolder = new SimpleBlobHolder(originalVideo);
        VideoConversion conversion = videoConversions.registry.get(conversionName);
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("height", conversion.getHeight());
        ConversionService conversionService = Framework.getLocalService(ConversionService.class);
        BlobHolder result = conversionService.convert(
                conversion.getConverter(), blobHolder, parameters);
        return result.getBlob();
    }

}
