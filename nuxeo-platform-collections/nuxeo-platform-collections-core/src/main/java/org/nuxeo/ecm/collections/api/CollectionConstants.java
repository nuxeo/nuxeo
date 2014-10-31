/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.api;

/**
 * @since 5.9.3
 */
public class CollectionConstants {

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

    public static final String MAGIC_PREFIX_ID = "-999999";

    public static final String DEFAULT_COLLECTIONS_NAME = "Collections";

    public static final String DEFAULT_COLLECTIONS_TITLE = "label.myCollections.title";

    public static final String COLLECTIONS_TYPE = "Collections";

    public static final String COLLECTION_QUEUE_ID = "collections";

    public static final int DEFAULT_COLLECTION_RETURNED = 10;

    public static final int MAX_COLLECTION_RETURNED = 100;

    public static final String COLLECTION_CONTENT_PAGE_PROVIDER = "default_content_collection";

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
