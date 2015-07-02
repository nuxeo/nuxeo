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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
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
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api
        .CommandLineExecutorService;
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
    public Blob writeMetadata(Blob blob, Map<String, Object> metadata, boolean ignorePrefix) {
        String command = ignorePrefix ? BinaryMetadataConstants.EXIFTOOL_WRITE_NOPREFIX
                : BinaryMetadataConstants.EXIFTOOL_WRITE;
        CommandAvailability ca = commandLineService.getCommandAvailability(command);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + command + "' is not available.");
        }
        if (blob == null) {
            throw new BinaryMetadataException("The following command " + ca + " cannot be executed with a null blob");
        }
        try {
            Blob newBlob = getTemporaryBlob(blob);
            CmdParameters params = new CmdParameters();
            params.addNamedParameter("inFilePath", newBlob.getFile(), true);
            params.addNamedParameter("tagList", getCommandTags(metadata), false);
            ExecResult er = commandLineService.execCommand(command, params);
            boolean success = er.isSuccessful();
            if (!success) {
                log.error("There was an error executing " + "the following command: " + er.getCommandLine() + ". \n"
                        + er.getOutput().get(0));
                return null;
            }
            newBlob.setMimeType(blob.getMimeType());
            newBlob.setEncoding(blob.getEncoding());
            newBlob.setFilename(blob.getFilename());
            return newBlob;
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new BinaryMetadataException("Command '" + command + "' is not available.", commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    protected Map<String, Object> readMetadata(String command, Blob blob, List<String> metadata, boolean ignorePrefix) {
        CommandAvailability ca = commandLineService.getCommandAvailability(command);
        if (!ca.isAvailable()) {
            throw new BinaryMetadataException("Command '" + command + "' is not available.");
        }
        if (blob == null) {
            throw new BinaryMetadataException("The following command " + ca + " cannot be executed with a null blob");
        }
        try {
            ExecResult er;
            try (CloseableFile source = getTemporaryFile(blob)) {
                CmdParameters params = new CmdParameters();
                params.addNamedParameter("inFilePath", source.getFile(), true);
                if (metadata != null) {
                    params.addNamedParameter("tagList", getCommandTags(metadata), false);
                }
                er = commandLineService.execCommand(command, params);
            }
            return returnResultMap(er);
        } catch (CommandNotAvailable commandNotAvailable) {
            throw new RuntimeException("Command '" + command + "' is not available.", commandNotAvailable);
        } catch (IOException ioException) {
            throw new BinaryMetadataException(ioException);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadata, boolean ignorePrefix) {
        String command = ignorePrefix ? BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST_NOPREFIX
                : BinaryMetadataConstants.EXIFTOOL_READ_TAGLIST;
        return readMetadata(command, blob, metadata, ignorePrefix);
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob, boolean ignorePrefix) {
        String command = ignorePrefix ? BinaryMetadataConstants.EXIFTOOL_READ_NOPREFIX
                : BinaryMetadataConstants.EXIFTOOL_READ;
        return readMetadata(command, blob, null, ignorePrefix);
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
            metadataValue = metadataValue.toString().replaceAll("'", "");
            sb.append("-" + metadata + "=" + metadataValue + " ");
        }
        return sb.toString();
    }

    protected Pattern VALID_EXT = Pattern.compile("[a-zA-Z0-9]*");

    /**
     * We don't want to rely on {@link Blob#getCloseableFile} because it may return the original and we always want a
     * temporary one to be sure we have a clean filename to pass.
     *
     * @since 7.4
     */
    protected CloseableFile getTemporaryFile(Blob blob) throws IOException {
        String ext = FilenameUtils.getExtension(blob.getFilename());
        if (!VALID_EXT.matcher(ext).matches()) {
            ext = "tmp";
        }
        File tmp = File.createTempFile("nxblob-", '.' + ext);
        File file = blob.getFile();
        if (file == null) {
            // if we don't have an underlying File, use a temporary File
            try (InputStream in = blob.getStream()) {
                Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            // attempt to create a symbolic link, which would be cheaper than a copy
            tmp.delete();
            try {
                Files.createSymbolicLink(tmp.toPath(), file.toPath().toAbsolutePath());
            } catch (IOException | UnsupportedOperationException e) {
                // symbolic link not supported, do a copy instead
                Files.copy(file.toPath(), tmp.toPath());
            }
        }
        return new CloseableFile(tmp, true);
    }

    /**
     * Gets a new blob on a temporary file which is a copy of the blob's.
     *
     * @since 7.4
     */
    protected Blob getTemporaryBlob(Blob blob) throws IOException {
        String ext = FilenameUtils.getExtension(blob.getFilename());
        if (!VALID_EXT.matcher(ext).matches()) {
            ext = "tmp";
        }
        Blob newBlob = new FileBlob('.' + ext);
        File tmp = newBlob.getFile();
        File file = blob.getFile();
        if (file == null) {
            try (InputStream in = blob.getStream()) {
                Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            // do a copy
            Files.copy(file.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return newBlob;
    }

}
