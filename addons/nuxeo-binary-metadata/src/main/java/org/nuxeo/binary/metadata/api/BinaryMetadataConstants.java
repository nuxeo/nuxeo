/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api;

/**
 * @since 7.1
 */
public class BinaryMetadataConstants {

    /**
     * Commandline id - read metadata from binary with exiftool.
     */
    public static final String EXIFTOOL_READ = "exiftool-read";

    /**
     * @since 7.3
     */
    public static final String EXIFTOOL_READ_NOPREFIX = "exiftool-read-noprefix";

    /**
     * Commandline id - read metadata listing from binary with exiftool.
     */
    public static final String EXIFTOOL_READ_TAGLIST = "exiftool-read-taglist";

    /**
     * @since 7.3
     */
    public static final String EXIFTOOL_READ_TAGLIST_NOPREFIX = "exiftool-read-taglist-noprefix";

    /**
     * Commandline id - Write metadata into binary with exiftool.
     */
    public static final String EXIFTOOL_WRITE = "exiftool-write";

    /**
     * @since 7.3
     */
    public static final String EXIFTOOL_WRITE_NOPREFIX = "exiftool-write-noprefix";

    public static final String METADATA_MAPPING_EP = "metadataMappings";

    public static final String METADATA_PROCESSORS_EP = "metadataProcessors";

    public static final String METADATA_RULES_EP = "metadataRules";

    public static final String EXIF_TOOL_CONTRIBUTION_ID = "exifTool";

    /**
     * Flag to disable binary metadata listener.
     */
    public static final String DISABLE_BINARY_METADATA_LISTENER = "disableBinaryMetadataListener";

    /**
     * Constant map key to do the async update of given metadata listing.
     *
     * @deprecated since 2021.13, not used anymore
     */
    @Deprecated
    public static final String ASYNC_MAPPING_RESULT = "asyncMappingResult";

    /**
     * Flag to execute the worker if async update should be done.
     *
     * @since 7.2
     * @deprecated since 2021.13, not used anymore
     */
    @Deprecated
    public static final String ASYNC_BINARY_METADATA_EXECUTE = "asyncExecute";

    /**
     * Binary Metadata configuration constant to active/deactivate metrics.
     *
     * @since 7.2
     */
    public static final String BINARY_METADATA_MONITOR = "binary.metadata.monitor.enable";
}
