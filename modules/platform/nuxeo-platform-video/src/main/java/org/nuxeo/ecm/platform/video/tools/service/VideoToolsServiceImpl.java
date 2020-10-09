/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.video.tools.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.video.tools.VideoClosedCaptionsExtractor;
import org.nuxeo.ecm.platform.video.tools.VideoConcat;
import org.nuxeo.ecm.platform.video.tools.VideoSlicer;
import org.nuxeo.ecm.platform.video.tools.VideoTool;
import org.nuxeo.ecm.platform.video.tools.VideoToolsService;
import org.nuxeo.ecm.platform.video.tools.VideoWatermarker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link VideoToolsService} default implementation for handling video blobs. It provides extension points for
 * handling video operations, such as concat, slice, watermark and extract closed captions.
 *
 * @since 8.4
 */
public class VideoToolsServiceImpl extends DefaultComponent implements VideoToolsService {

    protected static final Log log = LogFactory.getLog(VideoToolsServiceImpl.class);

    protected Map<String, Class<?>> videoTools;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);

        videoTools = new HashMap<>();
        videoTools.put(VideoWatermarker.NAME, VideoWatermarker.class);
        videoTools.put(VideoSlicer.NAME, VideoSlicer.class);
        videoTools.put(VideoConcat.NAME, VideoConcat.class);
        videoTools.put(VideoClosedCaptionsExtractor.NAME, VideoClosedCaptionsExtractor.class);
    }

    @Override
    public Blob extractClosedCaptions(Blob video, String outputFormat, String startAt, String endAt) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(VideoClosedCaptionsExtractor.OUTPUT_FORMAT_PARAM, outputFormat);
        parameters.put(VideoClosedCaptionsExtractor.START_AT_PARAM, startAt);
        parameters.put(VideoClosedCaptionsExtractor.END_AT_PARAM, endAt);

        BlobHolder result = execute(VideoClosedCaptionsExtractor.NAME, new SimpleBlobHolder(video), parameters);
        return result.getBlob();
    }

    @Override
    public Blob concat(List<Blob> videos) {
        BlobHolder blobHolder = execute(VideoConcat.NAME, new SimpleBlobHolder(videos), null);
        return blobHolder.getBlob();
    }

    @Override
    public List<Blob> slice(Blob video, String startAt, String duration, boolean encode) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(VideoSlicer.DURATION_PARAM, duration);
        parameters.put(VideoSlicer.START_AT_PARAM, startAt);
        parameters.put(VideoSlicer.ENCODE_PARAM, encode);

        BlobHolder result = execute(VideoSlicer.NAME, new SimpleBlobHolder(video), parameters);
        return result.getBlobs();
    }

    @Override
    public Blob watermark(Blob video, Blob picture, String x, String y) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(VideoWatermarker.WATERMARK_PARAM, picture);
        parameters.put(VideoWatermarker.WATERMARK_X_POSITION_PARAM, x);
        parameters.put(VideoWatermarker.WATERMARK_Y_POSITION_PARAM, y);

        BlobHolder result = execute(VideoWatermarker.NAME, new SimpleBlobHolder(video), parameters);
        return result.getBlob();
    }

    /**
     * Executes the video tool with the provided parameters.
     * @param toolName
     * @param blobHolder
     * @param parameters
     * @return
     */
    private BlobHolder execute(String toolName, BlobHolder blobHolder, Map<String, Object> parameters) {
        BlobHolder result = null;

        if (!isToolAvailable(toolName)) {
            throw new NuxeoException("The video tool '" + toolName + "' is not available");
        }
        try {
            // initialize the tool and set up the parameters
            VideoTool tool = (VideoTool) videoTools.get(toolName).getDeclaredConstructor().newInstance();
            Map<String, String> params = tool.setupParameters(blobHolder, parameters);
            CmdParameters cmdParams = setupCmdParameters(params);
            String commandLineName = tool.getCommandLineName();

            CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
            ExecResult clResult = cles.execCommand(commandLineName, cmdParams);
            tool.cleanupInputs(params);
            // Get the result, and first, handle errors.
            if (clResult.getError() != null) {
                throw new NuxeoException("Failed to execute the command <" + commandLineName + ">",
                        clResult.getError());
            }

            if (!clResult.isSuccessful()) {
                throw new NuxeoException("Failed to execute the command <" + commandLineName + ">. Final command [ "
                        + clResult.getCommandLine() + " ] returned with error " + clResult.getReturnCode());
            }
            result = tool.buildResult(blobHolder.getBlob().getMimeType(), params);
        } catch (CommandNotAvailable e) {
            throw new NuxeoException("The video tool command is not available.", e);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("The video tool is not available.", e);
        }
        return result;
    }

    @Override
    public boolean isToolAvailable(String toolName) {
        String commandLine;
        try {
            VideoTool tool = (VideoTool) videoTools.get(toolName).getDeclaredConstructor().newInstance();
            commandLine = tool.getCommandLineName();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("The video tool is not available.", e);
        }
        CommandAvailability ca = Framework.getService(CommandLineExecutorService.class)
                                          .getCommandAvailability(commandLine);
        return ca.isAvailable();
    }

    protected CmdParameters setupCmdParameters(Map<String, String> parameters) {
        CmdParameters cmdParameters = new CmdParameters();
        for (String param: parameters.keySet()) {
            cmdParameters.addNamedParameter(param, parameters.get(param));
        }
        return cmdParameters;
    }

}
