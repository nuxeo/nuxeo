/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.mimetype.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;

/**
 * MimetypeEntry registry service.
 * <p>
 * Singleton holding a registry of mimetype entries and exposes an API to grab information related to these mimetypes.
 * As well, this is possible to ask for a mimetype magic detection from a stream or file using the API.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class MimetypeRegistryService extends DefaultComponent implements MimetypeRegistry {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService");

    protected static final String MIMETYPE_XP = "mimetype";

    protected static final String EXTENSION_XP = "extension";

    // 10 MB is the max size to allow full file scan
    public static final long MAX_SIZE_FOR_SCAN = 10 * 1024 * 1024;

    public static final String TMP_EXTENSION = "tmp";

    public static final String MSOFFICE_TMP_PREFIX = "~$";

    protected MimetypeDescriptorRegistry getMimetypeRegistry() {
        return getExtensionPointRegistry(MIMETYPE_XP);
    }

    /**
     * @deprecated since 11.1. Use {@link #isMimeTypeNormalized(String)} instead.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    protected boolean isMimetypeEntry(String mimetypeName) {
        return isMimeTypeNormalized(mimetypeName);
    }

    @Override
    public List<String> getExtensionsFromMimetypeName(String mimetypeName) {
        return getMimetypeRegistry().getEntryKeys()
                                    .stream()
                                    .filter(e -> e.getValue().getMimetypes().contains(mimetypeName))
                                    .flatMap(e -> e.getValue().getExtensions().stream())
                                    .collect(Collectors.toList());
    }

    @Override
    public MimetypeEntry getMimetypeEntryByName(String name) {
        return getMimetypeRegistry().getEntryByName(name);
    }

    @Override
    public String getMimetypeFromFile(File file) {
        if (file.length() > MAX_SIZE_FOR_SCAN) {
            String exceptionMessage = "Not able to determine mime type from filename and file is too big for binary scan.";
            if (file.getAbsolutePath() == null) {
                throw new MimetypeNotFoundException(exceptionMessage);
            }
            try {
                return getMimetypeFromFilename(file.getAbsolutePath());
            } catch (MimetypeNotFoundException e) {
                throw new MimetypeNotFoundException(exceptionMessage, e);
            }
        }
        try {
            MagicMatch match = Magic.getMagicMatch(file, true, false);
            String mimeType;

            if (match.getSubMatches().isEmpty()) {
                mimeType = match.getMimeType();
            } else {
                // Submatches found
                // TODO: we only take the first here
                // what to do with other possible responses ?
                // b.t.w., multiple responses denotes a non-accuracy problem in
                // magic.xml but be careful to nested possible
                // sub-sub-...-submatches make this as recursive ?
                Collection<MagicMatch> possibilities = match.getSubMatches();
                Iterator<MagicMatch> iter = possibilities.iterator();
                MagicMatch m = iter.next();
                mimeType = m.getMimeType();
                // need to clean for subsequent calls
                possibilities.clear();
                match.setSubMatches(possibilities);
            }
            if ("text/plain".equals(mimeType)) {
                // check we didn't mis-detect files with zeroes
                // check first 16 bytes
                byte[] bytes = new byte[16];
                int n = 0;
                try (FileInputStream is = new FileInputStream(file)) {
                    n = is.read(bytes);
                }
                for (int i = 0; i < n; i++) {
                    if (bytes[i] == 0) {
                        mimeType = DEFAULT_MIMETYPE;
                        break;
                    }
                }
            }
            return mimeType;
        } catch (MagicMatchNotFoundException e) {
            if (file.getAbsolutePath() != null) {
                return getMimetypeFromFilename(file.getAbsolutePath());
            }
            throw new MimetypeNotFoundException(e.getMessage(), e);
        } catch (MagicException | MagicParseException | IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        }
    }

    @Override
    public String getMimetypeFromExtension(String extension) {
        String lowerCaseExtension = extension.toLowerCase();
        Optional<ExtensionDescriptor> optExtensionDescriptor = getRegistryContribution(EXTENSION_XP,
                lowerCaseExtension);
        if (optExtensionDescriptor.isEmpty()) {
            // no explicit extension rule, analyze the inverted mimetype
            // registry
            MimetypeEntry mimetype = getMimetypeRegistry().getEntryByExtension(lowerCaseExtension);
            if (mimetype == null) {
                throw new MimetypeNotFoundException("no registered mimetype has extension: " + lowerCaseExtension);
            } else {
                return mimetype.getNormalized();
            }
        } else {
            ExtensionDescriptor extensionDescriptor = optExtensionDescriptor.get();
            if (extensionDescriptor.isAmbiguous()) {
                throw new MimetypeNotFoundException(
                        String.format("mimetype for %s is ambiguous, binary sniffing needed", lowerCaseExtension));
            } else {
                return extensionDescriptor.getMimetype();
            }
        }
    }

    @Override
    public String getMimetypeFromFilename(String filename) {
        if (filename == null) {
            throw new MimetypeNotFoundException("filename is null");
        }
        if (isTemporaryFile(filename)) {
            return DEFAULT_MIMETYPE;
        }
        String extension = FilenameUtils.getExtension(filename);
        if (StringUtils.isBlank(extension)) {
            throw new MimetypeNotFoundException(filename + "has no extension");
        }
        return getMimetypeFromExtension(extension);
    }

    protected boolean isTemporaryFile(String filename) {
        return FilenameUtils.getExtension(filename).equalsIgnoreCase(TMP_EXTENSION)
                || FilenameUtils.getName(filename).startsWith(MSOFFICE_TMP_PREFIX);
    }

    @Override
    public String getMimetypeFromBlob(Blob blob) {
        if (blob.getLength() > MAX_SIZE_FOR_SCAN) {
            try {
                return getMimetypeFromFilename(blob.getFilename());
            } catch (MimetypeNotFoundException e) {
                throw new MimetypeNotFoundException("File is too big for binary scan");
            }
        }
        File file;
        try {
            file = Framework.createTempFile("NXMimetypeBean", ".bin");
            try (InputStream is = blob.getStream()) {
                FileUtils.copyInputStreamToFile(is, file);
                return getMimetypeFromFile(file);
            } finally {
                if (file != null) {
                    Files.delete(file.toPath());
                }
            }
        } catch (IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        }
    }

    @Override
    public MimetypeEntry getMimetypeEntryByMimeType(String mimetype) {
        return getMimetypeEntryByName(getNormalizedMimeType(mimetype).orElse(DEFAULT_MIMETYPE));
    }

    @Override
    public String getMimetypeFromBlobWithDefault(Blob blob, String defaultMimetype) {
        try {
            return getMimetypeFromBlob(blob);
        } catch (MimetypeNotFoundException e) {
            return defaultMimetype;
        }
    }

    @Override
    public String getMimetypeFromFilenameAndBlobWithDefault(String filename, Blob blob, String defaultMimetype) {
        try {
            return getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            // failed to detect mimetype on extension:
            // fallback to calculate mimetype from blob content
            return getMimetypeFromBlobWithDefault(blob, defaultMimetype);
        }
    }

    @Override
    public String getMimetypeFromFilenameWithBlobMimetypeFallback(String filename, Blob blob, String defaultMimetype) {
        try {
            return getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            // failed to detect mimetype on extension:
            // fallback to the blob defined mimetype
            String mimeTypeName = blob.getMimeType();
            if (isMimeTypeNormalized(mimeTypeName)) {
                return mimeTypeName;
            } else {
                // failed to detect mimetype on blob:
                // fallback to calculate mimetype from blob content
                return getMimetypeFromBlobWithDefault(blob, defaultMimetype);
            }
        }
    }

    @Override
    public Blob updateMimetype(Blob blob, String filename, Boolean withBlobMimetypeFallback) {
        if (filename == null) {
            filename = blob.getFilename();
        } else if (blob.getFilename() == null) {
            blob.setFilename(filename);
        }
        if (Boolean.TRUE.equals(withBlobMimetypeFallback)) {
            blob.setMimeType(getMimetypeFromFilenameWithBlobMimetypeFallback(filename, blob, DEFAULT_MIMETYPE));
        } else {
            blob.setMimeType(getMimetypeFromFilenameAndBlobWithDefault(filename, blob, DEFAULT_MIMETYPE));
        }
        return blob;
    }

    @Override
    public Blob updateMimetype(Blob blob, String filename) {
        return updateMimetype(blob, filename, false);
    }

    @Override
    public Blob updateMimetype(Blob blob) {
        return updateMimetype(blob, null);
    }

    @Override
    public Optional<String> getNormalizedMimeType(String mimeType) {
        if (mimeType == null) {
            return Optional.empty();
        }
        return getMimetypeRegistry().getEntryKeys()
                                    .stream()
                                    .filter(e -> e.getKey().equals(mimeType)
                                            || e.getValue().getMimetypes().contains(mimeType))
                                    .findAny()
                                    .map(Map.Entry::getKey);
    }

    @Override
    public boolean isMimeTypeNormalized(String mimeType) {
        return getMimetypeRegistry().isNormalized(mimeType);
    }

}
