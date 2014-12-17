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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 7.1
 */
public class ExifToolProcessor extends BinaryMetadataProcessor {

    private static final Log log = LogFactory.getLog(ExifToolProcessor.class);

    protected final ObjectMapper jacksonMapper;

    public ExifToolProcessor() {
        this.jacksonMapper = new ObjectMapper();
    }

    @Override
    public boolean writeMetadata(Blob blob, Map<String, String> metadata) {
        return false;
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadata) {
        CommandAvailability ca = getCommandLineExecutorService().getCommandAvailability(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST
                    + "' is not available.");
        }
        try {
            CmdParameters params = new CmdParameters();
            File file = makeFile(blob);
            params.addNamedParameter("inFilePath", file);
            params.addNamedParameter("tagList", getCommandTags(metadata));
            ExecResult er = getCommandLineExecutorService().execCommand(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST, params);
            if (!er.isSuccessful()) {
                throw new BinaryMetadataException("There was an error executing the following command: "
                        + er.getCommandLine());
            }
            StringBuilder sb = new StringBuilder();
            for (String line : er.getOutput()) {
                sb.append(line);
            }
            String jsonOutput = sb.toString();
            List<Map<String,Object>> map = jacksonMapper.readValue(jsonOutput, new TypeReference<List<HashMap<String, Object>>>() {
            });
            Map<String,Object> binaryMetadataMap = map.get(0);
            return binaryMetadataMap;
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new RuntimeException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST + "' is not available.",
                    commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    protected String getCommandTags(List<String> metadataList) {
        StringBuilder sb = new StringBuilder();
        for(String metadata: metadataList){
            sb.append("-" + metadata + " ");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        CommandAvailability ca = getCommandLineExecutorService().getCommandAvailability(BinaryMetadataConstants.EXIFTOOL_READ);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ
                    + "' is not available.");
        }
        try {
            CmdParameters params = new CmdParameters();
            File file = makeFile(blob);
            params.addNamedParameter("inFilePath", file);
            ExecResult er = getCommandLineExecutorService().execCommand(BinaryMetadataConstants.EXIFTOOL_READ, params);
            if (!er.isSuccessful()) {
                throw new BinaryMetadataException("There was an error executing the following command: "
                        + er.getCommandLine());
            }
            StringBuilder sb = new StringBuilder();
            for (String line : er.getOutput()) {
                sb.append(line);
            }
            String jsonOutput = sb.toString();
            List<Map<String,Object>> map = jacksonMapper.readValue(jsonOutput,new TypeReference<List<HashMap<String,Object>>>(){});
            return map.get(0);
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new RuntimeException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ + "' is not available.",
                    commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }
}
