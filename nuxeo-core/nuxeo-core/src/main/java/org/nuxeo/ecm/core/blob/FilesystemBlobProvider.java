/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;

/**
 * Blob provider that can reference files on the filesystem.
 * <p>
 * This blob provider MUST be configured with a "root" property that specifies the minimum root path for all files:
 *
 * <pre>
 * <code>
 * &lt;blobprovider name="myfsblobprovider">
 *   &lt;class>org.nuxeo.ecm.core.blob.FilesystemBlobProvider&lt;/class>
 *   &lt;property name="root">/base/directory/for/files&lt;/property>
 * &lt;/blobprovider>
 * </code>
 * </pre>
 * <p>
 * A root of {@code /} may be used to allow any path.
 * <p>
 * Blobs are constructed through {@link FilesystemBlobProvider#createBlob}. The constructed blob's key, which will be
 * stored in the document database, contains a path relative to the root.
 *
 * @since 7.10
 */
public class FilesystemBlobProvider extends AbstractBlobProvider {

    public static final String ROOT_PROP = "root";

    /** The root ending with /, or an empty string. */
    protected String root;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        root = properties.get(ROOT_PROP);
        if (StringUtils.isBlank(root)) {
            throw new NuxeoException(
                    "Missing property '" + ROOT_PROP + "' for " + getClass().getSimpleName() + ": " + blobProviderId);
        }
        if ("/".equals(root)) {
            root = "";
        } else if (!root.endsWith("/")) {
            root = root + "/";
        }
    }

    @Override
    public void close() {
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        return new SimpleManagedBlob(blobInfo);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        String key = blob.getKey();
        // strip prefix
        int colon = key.indexOf(':');
        if (colon >= 0 && key.substring(0, colon).equals(blobProviderId)) {
            key = key.substring(colon + 1);
        }
        // final sanity checks
        if (key.contains("..")) {
            throw new FileNotFoundException("Illegal path: " + key);
        }
        return Files.newInputStream(Paths.get(root + key));
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdateDefaultFalse();
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        throw new UnsupportedOperationException("Writing a blob is not supported");
    }

    /**
     * Creates a filesystem blob with the given information.
     * <p>
     * The passed {@link BlobInfo} contains information about the blob, and the key is a file path.
     *
     * @param blobInfo the blob info where the key is a file path
     * @return the blob
     */
    public ManagedBlob createBlob(BlobInfo blobInfo) throws IOException {
        String filePath = blobInfo.key;
        if (filePath.contains("..")) {
            throw new FileNotFoundException("Illegal path: " + filePath);
        }
        if (!filePath.startsWith(root)) {
            throw new FileNotFoundException("Path is not under configured root: " + filePath);
        }
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException(filePath);
        }
        // dereference links
        while (Files.isSymbolicLink(path)) {
            // dereference if link
            path = Files.readSymbolicLink(path);
            if (!Files.exists(path)) {
                throw new FileNotFoundException(filePath);
            }
        }
        String relativePath = filePath.substring(root.length());
        long length = Files.size(path);
        blobInfo = new BlobInfo(blobInfo); // copy
        blobInfo.key = blobProviderId + ":" + relativePath;
        blobInfo.length = Long.valueOf(length);
        if (blobInfo.filename == null) {
            blobInfo.filename = Paths.get(filePath).getFileName().toString();
        }
        if (blobInfo.digest == null) {
            try (InputStream in = Files.newInputStream(path)) {
                blobInfo.digest = DigestUtils.md5Hex(in);
            }
        }
        return new SimpleManagedBlob(blobInfo);
    }

}
