/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Commandline id - read metadata listing from binary with exiftool.
     */
    public static final String EXIFTOOL_READ_TAGLIST = "exiftool-read-taglist";

    /**
     * Commandline id - Write metadata into binary with exiftool.
     */
    public static final String EXIFTOOL_WRITE = "exiftool-write";

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
     */
    public static final String ASYNC_MAPPING_RESULT = "asyncMappingResult";

    /**
     * Flag to execute the worker if async update should be done.
     *
     * @since 7.2
     */
    public static final String ASYNC_BINARY_METADATA_EXECUTE = "asyncExecute";

    /**
     * Binary Metadata configuration constant to active/deactivate metrics.
     *
     * @since 7.2
     */
    public static final String BINARY_METADATA_MONITOR = "binary.metadata.monitor.enable";
}
