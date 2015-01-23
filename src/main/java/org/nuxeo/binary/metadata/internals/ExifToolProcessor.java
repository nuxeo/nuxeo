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
package org.nuxeo.binary.metadata.internals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.binary.metadata.api.BinaryMetadataProcessor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
public class ExifToolProcessor implements BinaryMetadataProcessor {

    private static final Log log = LogFactory.getLog(ExifToolProcessor.class);

    private static final String META_NON_USED_SOURCE_FILE = "SourceFile";

    protected final ObjectMapper jacksonMapper;

    protected final CommandLineExecutorService commandLineService;

    public ExifToolProcessor() {
        this.jacksonMapper = new ObjectMapper();
        this.commandLineService = Framework.getLocalService(CommandLineExecutorService.class);
    }

    @Override
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata) {
        CommandAvailability ca = commandLineService.getCommandAvailability(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_WRITE
                    + "' is not available.");
        }
        if (blob == null) {
            throw new BinaryMetadataException("The following command " + ca + " cannot be executed with a null blob");
        }
        try {
            ExecResult er;
            try (CloseableFile source = blob.getCloseableFile()) {
                CmdParameters params = new CmdParameters();
                params.addNamedParameter("inFilePath", source.getFile(), true);
                params.addNamedParameter("tagList", getCommandTags(metadata), false);
                er = commandLineService.execCommand(BinaryMetadataConstants.EXIFTOOL_WRITE, params);
            }
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
        CommandAvailability ca = commandLineService.getCommandAvailability(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST
                    + "' is not available.");
        }
        if (blob == null) {
            throw new BinaryMetadataException("The following command " + ca + " cannot be executed with a null blob");
        }
        try {
            ExecResult er;
            try (CloseableFile source = blob.getCloseableFile()) {
                CmdParameters params = new CmdParameters();
                params.addNamedParameter("inFilePath", source.getFile(), true);
                params.addNamedParameter("tagList", getCommandTags(metadata), false);
                er = commandLineService.execCommand(BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST, params);
            }
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
        CommandAvailability ca = commandLineService.getCommandAvailability(BinaryMetadataConstants.EXIFTOOL_READ);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + BinaryMetadataConstants.EXIFTOOL_READ
                    + "' is not available.");
        }
        if (blob == null) {
            throw new BinaryMetadataException("The following command " + ca + " cannot be executed with a null blob");
        }
        try {
            ExecResult er;
            try (CloseableFile source = blob.getCloseableFile()) {
                CmdParameters params = new CmdParameters();
                params.addNamedParameter("inFilePath", source.getFile(), true);
                er = commandLineService.execCommand(BinaryMetadataConstants.EXIFTOOL_READ, params);
            }
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
            Object metadataValue = metadataMap.get(metadata);
            if (metadataValue == null) {
                metadataValue = StringUtils.EMPTY;
            }
            metadataValue = metadataValue.toString().replace(" ", "\\ ");
            sb.append("-" + metadata + "=" + metadataValue + " ");
        }
        return sb.toString();
    }

}
