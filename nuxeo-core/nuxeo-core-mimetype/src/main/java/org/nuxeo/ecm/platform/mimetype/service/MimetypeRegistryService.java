/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
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

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService");

    // 10 MB is the max size to allow full file scan
    public static final long MAX_SIZE_FOR_SCAN = 10 * 1024 * 1024;

    private static final Log log = LogFactory.getLog(MimetypeRegistryService.class);

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
            if (contrib instanceof MimetypeDescriptor) {
                MimetypeDescriptor mimetypeDescriptor = (MimetypeDescriptor) contrib;
                registerMimetype(mimetypeDescriptor.getMimetype());
            } else if (contrib instanceof ExtensionDescriptor) {
                registerFileExtension((ExtensionDescriptor) contrib);
            }
        }
    }

    public void registerMimetype(MimetypeEntry mimetype) {
        log.debug("Registering mimetype: " + mimetype.getNormalized());
        mimetypeByNormalisedRegistry.put(mimetype.getNormalized(), mimetype);
        for (String extension : mimetype.getExtensions()) {
            mimetypeByExtensionRegistry.put(extension, mimetype);
        }
    }

    public void registerFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.debug("Registering file extension: " + extensionDescriptor.getName());
        extensionRegistry.put(extensionDescriptor.getName(), extensionDescriptor);
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs == null) {
            return;
        }
        for (Object contrib : contribs) {
            if (contrib instanceof MimetypeDescriptor) {
                MimetypeDescriptor mimetypeDescriptor = (MimetypeDescriptor) contrib;
                unregisterMimetype(mimetypeDescriptor.getNormalized());
            } else if (contrib instanceof ExtensionDescriptor) {
                ExtensionDescriptor extensionDescriptor = (ExtensionDescriptor) contrib;
                unregisterFileExtension(extensionDescriptor);
            }
        }
    }

    public void unregisterMimetype(String mimetypeName) {
        log.debug("Unregistering mimetype: " + mimetypeName);
        MimetypeEntry mimetype = mimetypeByNormalisedRegistry.get(mimetypeName);
        if (mimetype == null) {
            return;
        }
        List<String> extensions = mimetype.getExtensions();
        mimetypeByNormalisedRegistry.remove(mimetypeName);
        for (String extension : extensions) {
            // FIXME: equals always fails because types are incompatible.
            if (mimetype.getNormalized().equals(mimetypeByExtensionRegistry.get(extension))) {
                mimetypeByExtensionRegistry.remove(extension);
            }
        }
    }

    public void unregisterFileExtension(ExtensionDescriptor extensionDescriptor) {
        log.debug("Unregistering file extension: " + extensionDescriptor.getName());
        extensionRegistry.remove(extensionDescriptor.getName());
    }

    public RuntimeContext getContext() {
        return bundle;
    }

    @Override
    public List<String> getExtensionsFromMimetypeName(String mimetypeName) {
        List<String> extensions = new ArrayList<>();
        for (String key : mimetypeByNormalisedRegistry.keySet()) {
            MimetypeEntry mimetypeEntry = mimetypeByNormalisedRegistry.get(key);
            if (mimetypeEntry.getMimetypes().contains(mimetypeName)) {
                extensions.addAll(mimetypeEntry.getExtensions());
            }
        }
        return extensions;
    }

    @Override
    public MimetypeEntry getMimetypeEntryByName(String name) {
        return mimetypeByNormalisedRegistry.get(name);
    }

    @Override
    public String getMimetypeFromFile(File file) throws MimetypeNotFoundException, MimetypeDetectionException {
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
                        mimeType = "application/octet-stream";
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
    public String getMimetypeFromExtension(String extension) throws MimetypeNotFoundException {
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
    public String getMimetypeFromFilename(String filename) throws MimetypeNotFoundException {
        if (filename == null) {
            throw new MimetypeNotFoundException("filename is null");
        }
        String extension = FilenameUtils.getExtension(filename);
        if (StringUtils.isBlank(extension)) {
            throw new MimetypeNotFoundException(filename + "has no extension");
        }
        return getMimetypeFromExtension(extension);
    }

    @Override
    public String getMimetypeFromBlob(Blob blob) throws MimetypeNotFoundException, MimetypeDetectionException {
        File file;
        try {
            file = Framework.createTempFile("NXMimetypeBean", ".bin");
            try {
                try (InputStream is = blob.getStream()) {
                    FileUtils.copyToFile(is, file);
                }
                return getMimetypeFromFile(file);
            } finally {
                file.delete();
            }
        } catch (IOException e) {
            throw new MimetypeDetectionException(e.getMessage(), e);
        }
    }

    @Override
    public MimetypeEntry getMimetypeEntryByMimeType(String mimetype) {
        MimetypeEntry mtype = mimetypeByNormalisedRegistry.get("application/octet-stream");
        if (mimetype != null) {
            for (String key : mimetypeByNormalisedRegistry.keySet()) {
                MimetypeEntry entry = mimetypeByNormalisedRegistry.get(key);
                if (mimetype.equals(entry.getNormalized()) || entry.getMimetypes().contains(mimetype)) {
                    mtype = entry;
                    break;
                }
            }
        }
        return mtype;
    }

    @Override
    public String getMimetypeFromBlobWithDefault(Blob blob, String defaultMimetype) throws MimetypeDetectionException {
        try {
            return getMimetypeFromBlob(blob);
        } catch (MimetypeNotFoundException e) {
            return defaultMimetype;
        }
    }

    @Override
    public String getMimetypeFromFilenameAndBlobWithDefault(String filename, Blob blob, String defaultMimetype)
            throws MimetypeDetectionException {
        try {
            return getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            // failed to detect mimetype on extension:
            // fallback to calculate mimetype from blob content
            return getMimetypeFromBlobWithDefault(blob, defaultMimetype);
        }
    }

    @Override
    public String getMimetypeFromFilenameWithBlobMimetypeFallback(String filename, Blob blob, String defaultMimetype)
            throws MimetypeDetectionException {
        try {
            return getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            // failed to detect mimetype on extension:
            // fallback to the blob defined mimetype
            String mimeTypeName = blob.getMimeType();
            if (isMimetypeEntry(mimeTypeName)) {
                return mimeTypeName;
            } else {
                // failed to detect mimetype on blob:
                // fallback to calculate mimetype from blob content
                return getMimetypeFromBlobWithDefault(blob, defaultMimetype);
            }
        }
    }

    @Override
    public Blob updateMimetype(Blob blob, String filename, Boolean withBlobMimetypeFallback)
            throws MimetypeDetectionException {
        if (filename == null) {
            filename = blob.getFilename();
        } else if (blob.getFilename() == null) {
            blob.setFilename(filename);
        }
        if (withBlobMimetypeFallback) {
            blob.setMimeType(getMimetypeFromFilenameWithBlobMimetypeFallback(filename, blob, DEFAULT_MIMETYPE));
        } else {
            blob.setMimeType(getMimetypeFromFilenameAndBlobWithDefault(filename, blob, DEFAULT_MIMETYPE));
        }
        return blob;
    }

    @Override
    public Blob updateMimetype(Blob blob, String filename) throws MimetypeDetectionException {
        return updateMimetype(blob, filename, false);
    }

    @Override
    public Blob updateMimetype(Blob blob) throws MimetypeDetectionException {
        return updateMimetype(blob, null);
    }

}
