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
import java.util.Map;

import org.nuxeo.ecm.core.blob.AESBlobStore.EncryptingOutputStream;

/**
 * A blob provider that encrypts binaries on the filesystem using AES.
 * <p>
 * To encrypt a binary, an AES key is needed. This key can be retrieved from a keystore, or generated from a password
 * using PBKDF2 (in which case each stored file contains a different salt for security reasons). The on-disk file format
 * is described in {@link EncryptingOutputStream}.
 * <p>
 * The blob provider configuration holds the keystore information to retrieve the AES key, or the password that is used
 * to generate a per-file key using PBKDF2.
 * <p>
 * For keystore use, the following properties are available:
 * <ul>
 * <li>keyStoreType: the keystore type, for instance JCEKS
 * <li>keyStoreFile: the path to the keystore, if applicable
 * <li>keyStorePassword: the keystore password
 * <li>keyAlias: the alias (name) of the key in the keystore
 * <li>keyPassword: the key password
 * </ul>
 * <p>
 * And for PBKDF2 use:
 * <ul>
 * <li>password: the password
 * </ul>
 * <p>
 * For backward compatibility, the properties can also be included in the
 * {@code <property name="key">prop1=value1,prop2=value2,...</property>} of the blob provider configuration.
 *
 * @since 11.1
 */
public class AESBlobProvider extends LocalBlobProvider {

    protected AESBlobStoreConfiguration aesConfig;

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        aesConfig = new AESBlobStoreConfiguration(properties);
        return super.getBlobStore(blobProviderId, properties);
    }

    @Override
    protected BlobStore newBlobStore(String name, KeyStrategy keyStrategy, PathStrategy pathStrategy) {
        return new AESBlobStore(name, keyStrategy, pathStrategy, aesConfig);
    }

}
