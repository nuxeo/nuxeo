/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for a {@link BlobProvider}.
 */
@XObject(value = "blobprovider")
public class BlobProviderDescriptor implements Descriptor {

    public static final String PREVENT_USER_UPDATE = "preventUserUpdate";

    /**
     * An optional namespace that may be used to disambiguate otherwise similar descriptors (in particular, copies).
     *
     * @since 10.10
     */
    public static final String NAMESPACE = "namespace";

    /**
     * Flags this blob provider as transient: blobs may disappear after a while, so a caller should not rely on them
     * being available forever.
     *
     * @since 10.1
     */
    public static final String TRANSIENT = "transient";

    /**
     * Flags this blob provider as using "cold storage mode".
     * <p>
     * Cold storage mode has the following characteristics:
     * <ul>
     * <li>transactional (blobs aren't actually written/deleted until the transaction commits, and transaction rollback
     * is possible)
     * </ul>
     *
     * @since 2021.19
     */
    public static final String COLD_STORAGE = "coldStorage";

    /**
     * Flags this blob provider as using "record mode".
     * <p>
     * Record mode has the following characteristics:
     * <ul>
     * <li>transactional (blobs aren't actually written/deleted until the transaction commits, and transaction rollback
     * is possible),
     * <li>can replace or delete a document's blob.
     * </ul>
     *
     * @since 11.1
     */
    public static final String RECORD = "record";

    /**
     * Flags this blob provider as transactional.
     * <p>
     * A transactional blob provider only writes blobs to final storage at commit time.
     *
     * @since 11.1
     */
    public static final String TRANSACTIONAL = "transactional";

    /**
     * Flags this blob provider as allowing internal queries with keys containing a byte range.
     *
     * @since 11.1
     */
    public static final String ALLOW_BYTE_RANGE = "allowByteRange";

    /**
     * Flags this blob provider as allowing direct download with {@link URI} returned by
     * {@link BlobProvider#getURI(ManagedBlob, org.nuxeo.ecm.core.blob.BlobManager.UsageHint, jakarta.servlet.http.HttpServletRequest)}.
     *
     * @since 2023.7
     */
    public static final String DIRECTDOWNLOAD_PROPERTY = "directdownload";

    /**
     * Expiration duration of a direct download link (see {@link #DIRECTDOWNLOAD_PROPERTY}) in seconds.
     *
     * @since 2023.7
     */
    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY = "directdownload.expire";

    /**
     * A comma-separated list of users that can create blobs in this blob provider based only on a key.
     *
     * @since 10.2
     */
    public static final String CREATE_FROM_KEY_USERS = "createFromKey.users";

    /**
     * A comma-separated list of groups that can create blobs in this blob provider based only on a key.
     *
     * @since 10.2
     */
    public static final String CREATE_FROM_KEY_GROUPS = "createFromKey.groups";

    @XNode("@name")
    public String name = "";

    @XNode("class")
    public Class<?> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<>();

    public BlobProviderDescriptor() {
    }

    /** Copy constructor. */
    public BlobProviderDescriptor(BlobProviderDescriptor other) {
        name = other.name;
        klass = other.klass;
        properties = new HashMap<>(other.properties);
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public BlobProviderDescriptor merge(Descriptor o) {
        var other = (BlobProviderDescriptor) o;
        var merged = new BlobProviderDescriptor();
        merged.name = name; // we merge based on name, so no name merging needed
        merged.klass = getIfNull(other.klass, klass);
        merged.properties.putAll(properties);
        merged.properties.putAll(other.properties);
        return merged;
    }

}
