/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Base class to implement {@link Converter} based on {@link CommandLineExecutorService}.
 *
 * @author tiry
 */
public abstract class CommandLineBasedConverter implements ExternalConverter {

    protected static final String CMD_NAME_PARAMETER = "CommandLineName";

    protected static final String TMP_PATH_PARAMETER = "TmpDirectory";

    protected Map<String, String> initParameters;

    /**
     * @deprecated Since 7.4. Useless.
     */
    @Deprecated
    protected CommandLineExecutorService getCommandLineService() {
        return Framework.getService(CommandLineExecutorService.class);
    }

    public String getTmpDirectory(Map<String, Serializable> parameters) {
        String tmp = initParameters.get(TMP_PATH_PARAMETER);
        if (parameters != null && parameters.containsKey(TMP_PATH_PARAMETER)) {
            tmp = (String) parameters.get(TMP_PATH_PARAMETER);
        }
        if (tmp == null) {
            tmp = Environment.getDefault().getTemp().getPath();
        }
        return tmp;
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        String commandName = getCommandName(blobHolder, parameters);
        if (commandName == null) {
            throw new ConversionException("Unable to determine target CommandLine name");
        }

        Map<String, Blob> blobParams = getCmdBlobParameters(blobHolder, parameters);
        Map<String, String> strParams = getCmdStringParameters(blobHolder, parameters);

        CmdReturn result = execOnBlob(commandName, blobParams, strParams);
        return buildResult(result.output, result.params);
    }

    protected String getCommandName(BlobHolder blobHolder, Map<String, Serializable> parameters) {
        String commandName = initParameters.get(CMD_NAME_PARAMETER);
        if (parameters != null && parameters.containsKey(CMD_NAME_PARAMETER)) {
            commandName = (String) parameters.get(CMD_NAME_PARAMETER);
        }
        return commandName;
    }

    /**
     * Extracts BlobParameters.
     */
    protected abstract Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Extracts String parameters.
     */
    protected abstract Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException;

    /**
     * Builds result from commandLine output buffer.
     */
    protected abstract BlobHolder buildResult(List<String> cmdOutput, CmdParameters cmdParams)
            throws ConversionException;

    protected class CmdReturn {
        protected final CmdParameters params;

        protected final List<String> output;

        protected CmdReturn(CmdParameters params, List<String> output) {
            this.params = params;
            this.output = output;
        }
    }

    protected CmdReturn execOnBlob(String commandName, Map<String, Blob> blobParameters, Map<String, String> parameters)
            throws ConversionException {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        List<Closeable> toClose = new ArrayList<>();
        try {
            if (blobParameters != null) {
                for (String blobParamName : blobParameters.keySet()) {
                    Blob blob = blobParameters.get(blobParamName);
                    // closed in finally block
                    CloseableFile closeable = blob.getCloseableFile("."
                            + FilenameUtils.getExtension(blob.getFilename()));
                    params.addNamedParameter(blobParamName, closeable.getFile());
                    toClose.add(closeable);
                }
            }

            if (parameters != null) {
                for (String paramName : parameters.keySet()) {
                    params.addNamedParameter(paramName, parameters.get(paramName));
                }
            }

            ExecResult result = Framework.getService(CommandLineExecutorService.class).execCommand(commandName, params);
            if (!result.isSuccessful()) {
                throw result.getError();
            }
            return new CmdReturn(params, result.getOutput());
        } catch (CommandNotAvailable e) {
            // XXX bubble installation instructions
            throw new ConversionException("Unable to find targetCommand", e);
        } catch (IOException | CommandException e) {
            throw new ConversionException("Error while converting via CommandLineService", e);
        } finally {
            for (Closeable closeable : toClose) {
                IOUtils.closeQuietly(closeable);
            }
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        initParameters = descriptor.getParameters();
        if (initParameters == null) {
            initParameters = new HashMap<>();
        }
    }

    @Override
    public ConverterCheckResult isConverterAvailable() {
        String commandName = getCommandName(null, null);
        if (commandName == null) {
            // can not check
            return new ConverterCheckResult();
        }

        CommandAvailability ca = Framework.getService(CommandLineExecutorService.class).getCommandAvailability(
                commandName);

        if (ca.isAvailable()) {
            return new ConverterCheckResult();
        } else {
            return new ConverterCheckResult(ca.getInstallMessage(), ca.getErrorMessage());
        }
    }

}
