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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerRootDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Configuration for the local storage of files.
 *
 * @since 11.1
 */
public class LocalBlobStoreConfiguration extends PropertyBasedConfiguration {

    /** In the initialization properties, the property for the store path. */
    public static final String PROP_PATH = "path";

    public static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("[a-zA-Z]:[/\\\\].*");

    public static final String DEFAULT_PATH = "binaries";

    public static final String DATA = "data";

    public static final String TMP = "tmp";

    public static final String CONFIG_FILE = "config.xml";

    public final Path storageDir;

    public final Path tmpDir;

    public final BinaryManagerRootDescriptor descriptor;

    public final DigestConfiguration digestConfiguration;

    public LocalBlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(null, properties);
        Path base = getStorageBase();
        storageDir = base.resolve(DATA);
        tmpDir = base.resolve(TMP);
        Files.createDirectories(storageDir);
        Files.createDirectories(tmpDir);
        descriptor = getDescriptor(base.resolve(CONFIG_FILE), true);
        digestConfiguration = new DigestConfiguration(null, properties);
    }

    /**
     * Gets the storage base to use, based on the properties.
     */
    public Path getStorageBase() {
        String path = getProperty(PROP_PATH, DEFAULT_PATH);
        path = Framework.expandVars(path);
        path = path.trim();
        Path base;
        if (path.startsWith("/") || path.startsWith("\\") || path.contains("://") || path.contains(":\\")
                || WINDOWS_ABSOLUTE_PATH.matcher(path).matches()) {
            // absolute
            base = Paths.get(path);
        } else {
            // relative
            Path home = Environment.getDefault().getData().toPath();
            base = home.resolve(path).normalize();
        }
        // take namespace into account
        String namespace = getProperty(BlobProviderDescriptor.NAMESPACE);
        if (StringUtils.isNotBlank(namespace)) {
            base = base.resolveSibling(base.getFileName() + "_" + namespace.trim());
        }
        return base;
    }

    public static final int DEFAULT_DEPTH = 2;

    /**
     * Gets existing descriptor or creates a default one.
     */
    public BinaryManagerRootDescriptor getDescriptor(Path configFile, boolean create) throws IOException {
        BinaryManagerRootDescriptor desc;
        if (Files.exists(configFile)) { // NOSONAR (squid:S3725)
            XMap xmap = new XMap();
            xmap.register(BinaryManagerRootDescriptor.class);
            desc = (BinaryManagerRootDescriptor) xmap.load(Files.newInputStream(configFile));
        } else {
            desc = new BinaryManagerRootDescriptor();
            desc.digest = DigestConfiguration.DEFAULT_DIGEST_ALGORITHM;
            desc.depth = DEFAULT_DEPTH;
            if (create) {
                desc.write(configFile.toFile()); // may throw IOException
            }
        }
        return desc;
    }

}
