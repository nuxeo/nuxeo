/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.binary.metadata.api.BinaryMetadataProcessor;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 7.1
 */
public class ExifToolProcessor implements BinaryMetadataProcessor {

    private static final Log log = LogFactory.getLog(ExifToolProcessor.class);

    private static final String META_NON_USED_SOURCE_FILE = "SourceFile";

    private static final String DATE_FORMAT_PATTERN = "yyyy:MM:dd HH:mm:ss";

    private static final String EXIF_IMAGE_DATE_TIME = "EXIF:DateTime";

    private static final String EXIF_PHOTO_DATE_TIME_ORIGINAL = "EXIF:DateTimeOriginal";

    private static final String EXIF_PHOTO_DATE_TIME_DIGITIZED = "EXIF:DateTimeDigitized";

    protected final ObjectMapper jacksonMapper;

    protected final CommandLineExecutorService commandLineService;

    public ExifToolProcessor() {
        jacksonMapper = new ObjectMapper();
        commandLineService = Framework.getService(CommandLineExecutorService.class);
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
            CmdParameters params = commandLineService.getDefaultCmdParameters();
            params.addNamedParameter("inFilePath", newBlob.getFile());
            params.addNamedParameter("tagList", getCommandTags(metadata));
            ExecResult er = commandLineService.execCommand(command, params);
            boolean success = er.isSuccessful();
            if (!success) {
                log.error("There was an error executing " + "the following command: " + er.getCommandLine() + ". \n"
                        + er.getOutput());
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
                CmdParameters params = commandLineService.getDefaultCmdParameters();
                params.addNamedParameter("inFilePath", source.getFile());
                if (metadata != null) {
                    params.addNamedParameter("tagList", getCommandTags(metadata));
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
            throw new BinaryMetadataException(
                    "There was an error executing " + "the following command: " + er.getCommandLine(), er.getError());
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
        parseDates(resultMap);
        return resultMap;
    }

    /**
     * @since 7.4
     */
    protected void parseDates(Map<String, Object> resultMap) {
        for (String prop : new String[] { EXIF_IMAGE_DATE_TIME, EXIF_PHOTO_DATE_TIME_ORIGINAL,
                EXIF_PHOTO_DATE_TIME_DIGITIZED }) {
            if (resultMap.containsKey(prop)) {
                Object dateObject = resultMap.get(prop);
                if (dateObject instanceof String) {
                    SimpleDateFormat f = new SimpleDateFormat(DATE_FORMAT_PATTERN);
                    try {
                        Date date = f.parse((String) dateObject);
                        resultMap.put(prop, date);
                    } catch (ParseException e) {
                        log.error("Could not parse property " + prop, e);
                    }
                }
            }
        }
    }

    protected List<String> getCommandTags(List<String> metadataList) {
        return metadataList.stream().map(tag -> "-" + tag).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getCommandTags(Map<String, Object> metadataMap) {
        List<String> commandTags = new ArrayList<>();
        for (String tag : metadataMap.keySet()) {
            Object metadataValue = metadataMap.get(tag);
            if (metadataValue instanceof Collection) {
                commandTags.addAll(buildCommandTagsFromCollection(tag, (Collection<Object>) metadataValue));
            } else if (metadataValue instanceof Object[]) {
                commandTags.addAll(buildCommandTagsFromCollection(tag, Arrays.asList((Object[]) metadataValue)));
            } else if (metadataValue instanceof Calendar) {
                commandTags.add(buildCommandTagFromDate(tag, ((Calendar) metadataValue).getTime()));
            } else {
                commandTags.add(buildCommandTag(tag, metadataValue));
            }
        }
        return commandTags;
    }

    /**
     * @since 8.3
     */
    private String buildCommandTag(String tag, Object value) {
        return "-" + tag + "=" + ObjectUtils.toString(value);
    }

    /**
     * @since 8.3
     */
    private List<String> buildCommandTagsFromCollection(String tag, Collection<Object> values) {
        return values.isEmpty() ? Collections.singletonList("-" + tag + "=")
                : values.stream().map(val -> buildCommandTag(tag, val)).collect(Collectors.toList());
    }

    /**
     * @since 8.4
     */
    private String buildCommandTagFromDate(String tag, Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return "-" + tag + "=" + formatter.format(date);
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
        File tmp = Framework.createTempFile("nxblob-", '.' + ext);
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
