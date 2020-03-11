/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a {@link BlobProvider}.
 */
@XObject(value = "blobprovider")
public class BlobProviderDescriptor {

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

    public void merge(BlobProviderDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
        properties.putAll(other.properties);
    }

}
