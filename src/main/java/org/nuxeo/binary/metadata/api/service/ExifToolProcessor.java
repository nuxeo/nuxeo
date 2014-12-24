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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;

/**
 * @since 7.1
 */
public class ExifToolProcessor extends BinaryMetadataProcessor {

    private static final Log log = LogFactory.getLog(ExifToolProcessor.class);

    private static final String META_NON_USED_SOURCE_FILE = "SourceFile";

    protected final ObjectMapper jacksonMapper;

    public ExifToolProcessor() {
        this.jacksonMapper = new ObjectMapper();
    }

    @Override
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata) {
        CommandAvailability ca = getCommandLineExecutorService().getCommandAvailability(
                BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_WRITE
                    + "' is not available.");
        }
        try {
            CmdParameters params = new CmdParameters();
            File file = makeFile(blob);
            params.addNamedParameter("inFilePath", file);
            params.addNamedParameter("tagList", getCommandTags(metadata));
            ExecResult er = getCommandLineExecutorService().execCommand(BinaryMetadataConstants.EXIFTOOL_WRITE, params,
                    false);
            boolean success = er.isSuccessful();
            if (!success) {
                log.error("There was an error executing " + "the following command: " + er.getCommandLine() + ". \n"
                        + er.getOutput().get(0));
            }
            return success;
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_WRITE
                    + "' is not available.", commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadata) {
        CommandAvailability ca = getCommandLineExecutorService().getCommandAvailability(
                BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST
                    + "' is not available.");
        }
        try {
            CmdParameters params = new CmdParameters();
            File file = makeFile(blob);
            params.addNamedParameter("inFilePath", file);
            params.addNamedParameter("tagList", getCommandTags(metadata));
            ExecResult er = getCommandLineExecutorService().execCommand(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST,
                    params, false);

            return returnResultMap(er);
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new RuntimeException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST
                    + "' is not available.", commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        CommandAvailability ca = getCommandLineExecutorService().getCommandAvailability(
                BinaryMetadataConstants.EXIFTOOL_READ);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ
                    + "' is not available.");
        }
        try {
            CmdParameters params = new CmdParameters();
            File file = makeFile(blob);
            params.addNamedParameter("inFilePath", file);
            ExecResult er = getCommandLineExecutorService().execCommand(BinaryMetadataConstants.EXIFTOOL_READ, params);

            return returnResultMap(er);
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new RuntimeException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ + "' is not available.",
                    commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    /*--------------------------- Utils ------------------------*/

    protected Map<String, Object> returnResultMap(ExecResult er) throws IOException {
        if (!er.isSuccessful()) {
            throw new BinaryMetadataException("There was an error executing " + "the following command: "
                    + er.getCommandLine(), er.getError());
        }
        StringBuilder sb = new StringBuilder();
        for (String line : er.getOutput()) {
            sb.append(line);
        }
        String jsonOutput = sb.toString();
        List<Map<String, Object>> resultList = jacksonMapper.readValue(jsonOutput,
                new TypeReference<List<HashMap<String, Object>>>() {
                });
        Map<String, Object> resultMap = resultList.get(0);
        // Remove the SourceFile metadata injected automatically by ExifTool.
        resultMap.remove(META_NON_USED_SOURCE_FILE);
        return resultMap;
    }

    protected String getCommandTags(List<String> metadataList) {
        StringBuilder sb = new StringBuilder();
        for (String metadata : metadataList) {
            sb.append("-" + metadata + " ");
        }
        return sb.toString();
    }

    protected String getCommandTags(Map<String, Object> metadataMap) {
        StringBuilder sb = new StringBuilder();
        for (String metadata : metadataMap.keySet()) {
            sb.append("-" + metadata + "=" + metadataMap.get(metadata).toString() + " ");
        }
        return sb.toString();
    }

}
