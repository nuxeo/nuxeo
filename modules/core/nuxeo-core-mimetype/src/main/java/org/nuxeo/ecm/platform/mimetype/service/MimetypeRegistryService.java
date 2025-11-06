/*
 * (C) Copyright 2006-2022 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;

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

    private static final Logger log = LogManager.getLogger(MimetypeRegistryService.class);

    public static final ComponentName NAME = new ComponentName( // NOSONAR
            "org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService");

    // the max size to allow full file scan
    public static final long MAX_SIZE_FOR_SCAN = ByteSize.ofMebibytes(10).toBytes();

    public static final String TMP_EXTENSION = "tmp";

    public static final String MSOFFICE_TMP_PREFIX = "~$";

    protected Map<String, MimetypeEntry> mimetypeByNormalisedRegistry;

    protected Map<String, MimetypeEntry> mimetypeByExtensionRegistry;

    protected Map<String, ExtensionDescriptor> extensionRegistry;

    private RuntimeContext bundle;

    public MimetypeRegistryService() {
        initializeRegistries();
    }

    protected void initializeRegistries() {
        mimetypeByNormalisedRegistry = new HashMap<>();
        mimetypeByExtensionRegistry = new HashMap<>();
        extensionRegistry = new HashMap<>();
    }

    /**
     * @deprecated since 11.1. Use {@link #isMimeTypeNormalized(String)} instead.
     */
    @Deprecated(since = "11.1", forRemoval = true)
    protected boolean isMimetypeEntry(String mimetypeName) {
        return mimetypeByNormalisedRegistry.containsKey(mimetypeName);
    }

    @Override
    public void activate(ComponentContext context) {
        bundle = context.getRuntimeContext();
        initializeRegistries();
    }

    @Override
    public void deactivate(ComponentContext context) {
        mimetypeByNormalisedRegistry = null;
        mimetypeByExtensionRegistry = null;
        extensionRegistry = null;
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof MimetypeDescriptor mimetypeDescriptor) {
                registerMimetype(mimetypeDescriptor.getMimetype());
            } else if (contrib instanceof ExtensionDescriptor extensionDescriptor) {
                registerFileExtension(extensionDescriptor);
            }
        }
    }

    public void registerMimetype(MimetypeEntry mimetype) {
        log.debug("Registering mimetype: {}", mimetype::getNormalized);
        mimetypeByNormalisedRegistry.put(mimetype.getNormalized(), mimetype);
        for (String extension : mimetype.getExtensions()) {
            mimetypeByExtensionRegistry.put(extension, mimetype);
        }
    }

    public void registerFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.debug("Registering file extension: {}", extensionDescriptor::getName);
        extensionRegistry.put(extensionDescriptor.getName(), extensionDescriptor);
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof MimetypeDescriptor mimetypeDescriptor) {
                unregisterMimetype(mimetypeDescriptor.getNormalized());
            } else if (contrib instanceof ExtensionDescriptor extensionDescriptor) {
                unregisterFileExtension(extensionDescriptor);
            }
        }
    }

    public void unregisterMimetype(String mimetypeName) {
        log.debug("Unregistering mimetype: {}", mimetypeName);
        MimetypeEntry mimetype = mimetypeByNormalisedRegistry.get(mimetypeName);
        if (mimetype == null) {
            return;
        }
        List<String> extensions = mimetype.getExtensions();
        mimetypeByNormalisedRegistry.remove(mimetypeName);
        for (String extension : extensions) {
            mimetypeByExtensionRegistry.remove(extension);
        }
    }

    public void unregisterFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.debug("Unregistering file extension: {}", extensionDescriptor::getName);
        extensionRegistry.remove(extensionDescriptor.getName());
    }

    public RuntimeContext getContext() {
        return bundle;
    }

    @Override
    public List<String> getExtensionsFromMimetypeName(String mimetypeName) {
        return mimetypeByNormalisedRegistry.entrySet()
                                           .stream()
                                           .filter(e -> e.getValue().getMimetypes().contains(mimetypeName))
                                           .flatMap(e -> e.getValue().getExtensions().stream())
                                           .toList();
    }

    @Override
    public MimetypeEntry getMimetypeEntryByName(String name) {
        return mimetypeByNormalisedRegistry.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getMimetypeFromFile(File file) {
        if (file.length() > MAX_SIZE_FOR_SCAN) {
            try {
                return getMimetypeFromFilename(file.getAbsolutePath());
            } catch (MimetypeNotFoundException e) {
                throw new MimetypeNotFoundException(
                        "Not able to determine mime type from filename and file is too big for binary scan.", e);
            }
        }
        try {
            MagicMatch match = Magic.getMagicMatch(file, true, false);
            if (match == null) {
                throw new MagicMatchNotFoundException();
            }
            // handle MagicMatch undefined MIME type
            if (UNDEFINED_MIMETYPE.equals(match.getMimeType())) {
                match.setMimeType(DEFAULT_MIMETYPE);
            }

            // Only take into account the first possibility.
            var possibilities = new ArrayList<MagicMatch>(match.getSubMatches());
            var possibility = possibilities.isEmpty() ? null : possibilities.get(0);
            MagicMatch m = requireNonNullElse(possibility, match);
            String mimeType = m.getMimeType();

            if ("text/plain".equals(mimeType)) {
                // check we didn't mis-detect files with zeroes
                // check first 16 bytes
                byte[] bytes = new byte[16];
                int n;
                try (FileInputStream is = new FileInputStream(file)) {
                    n = is.read(bytes);
                }
                for (int i = 0; i < n; i++) {
                    if (bytes[i] == 0) {
                        return DEFAULT_MIMETYPE;
                    }
                }
                // MagicMatch wrongly parses XML with attributes in the xml declaration as text/plain
                // MagicMatch is old and not maintained so this is a frugal effort to patch things up.
                if (new String(bytes, UTF_8).startsWith("<?xml ")) {
                    return XML_MIMETYPE;
                }
            }
            return mimeType;
        } catch (MagicMatchNotFoundException e) {
            return getMimetypeFromFilename(file.getAbsolutePath());
        } catch (MagicException | MagicParseException | IOException e) {
            throw new MimetypeDetectionException("Not able to determine mimetype from binary scan", e);
        }
    }

    @Override
    public String getMimetypeFromExtension(String extension) {
        String lowerCaseExtension = extension.toLowerCase();
        ExtensionDescriptor extensionDescriptor = extensionRegistry.get(lowerCaseExtension);
        if (extensionDescriptor == null) {
            // no explicit extension rule, analyse the inverted mimetype
            // registry
            MimetypeEntry mimetype = mimetypeByExtensionRegistry.get(lowerCaseExtension);
            if (mimetype == null) {
                throw new MimetypeNotFoundException("no registered mimetype has extension: " + lowerCaseExtension);
            } else {
                return mimetype.getNormalized();
            }
        } else {
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
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        }
    }

    @Override
    public MimetypeEntry getMimetypeEntryByMimeType(String mimetype) {
        String normalized = getNormalizedMimeType(mimetype).orElse(DEFAULT_MIMETYPE);
        return mimetypeByNormalisedRegistry.get(normalized);
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

        Set<Map.Entry<String, MimetypeEntry>> entries = mimetypeByNormalisedRegistry.entrySet();
        return entries.stream()
                      .filter(e -> e.getKey().equals(mimeType) || e.getValue().getMimetypes().contains(mimeType))
                      .findAny()
                      .map(Map.Entry::getKey);
    }

    @Override
    public boolean isMimeTypeNormalized(String mimeType) {
        return mimetypeByNormalisedRegistry.containsKey(mimeType);
    }
}
