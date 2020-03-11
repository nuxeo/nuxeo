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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.api;

/**
 * @since 5.9.3
 */
public class CollectionConstants {

    public static final String DISABLE_NOTIFICATION_SERVICE = "disableNotificationService";

    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    public static final String COLLECTABLE_FACET = "CollectionMember";

    public static final String NOT_COLLECTABLE_FACET = "NotCollectionMember";

    public static final String COLLECTION_FACET = "Collection";

    public static final String COLLECTION_TYPE = "Collection";

    public static final String COLLECTION_DOCUMENT_IDS_PROPERTY_NAME = "collection:documentIds";

    public static final String DOCUMENT_COLLECTION_IDS_PROPERTY_NAME = "collectionMember:collectionIds";

    public static final String COLLECTION_SCHEMA_NAME = "collection";

    public static final String COLLECTION_MEMBER_SCHEMA_NAME = "collectionMember";

    public static final String CAN_COLLECT_PERMISSION = "ReadCanCollect";

    public static final String COLLECTION_PAGE_PROVIDER = "default_collection";

    /**
     * @since 8.3
     */
    public static final String USER_COLLECTION_PAGE_PROVIDER = "user_collections";

    public static final String MAGIC_PREFIX_ID = "-999999";

    public static final String DEFAULT_COLLECTIONS_NAME = "Collections";

    public static final String DEFAULT_COLLECTIONS_TITLE = "label.myCollections.title";

    public static final String COLLECTIONS_TYPE = "Collections";

    public static final String COLLECTION_QUEUE_ID = "collections";

    public static final int DEFAULT_COLLECTION_RETURNED = 10;

    public static final int MAX_COLLECTION_RETURNED = 100;

    public static final String COLLECTION_CONTENT_PAGE_PROVIDER = "default_content_collection";

    /**
     * @since 8.4
     */
    public static final String ORDERED_COLLECTION_CONTENT_PAGE_PROVIDER = "ordered_content_collection";

    /**
     * @since 6.0
     */
    public static final String ALL_COLLECTIONS_PAGE_PROVIDER = "all_collections";

    // Event names
    /**
     * @since 6.0
     */
    public static final String BEFORE_ADDED_TO_COLLECTION = "beforeAddedToCollection";

    /**
     * @since 6.0
     */
    public static final String ADDED_TO_COLLECTION = "addedToCollection";

    /**
     * @since 6.0
     */
    public static final String BEFORE_REMOVED_FROM_COLLECTION = "beforeRemovedFromCollection";

    /**
     * @since 6.0
     */
    public static final String REMOVED_FROM_COLLECTION = "removedFromCollection";

    /**
     * @since 6.0
     */
    public static final String COLLECTION_REF_EVENT_CTX_PROP = "collectionRef";

}
