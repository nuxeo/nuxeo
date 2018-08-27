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
package org.nuxeo.ecm.collections.core.listener;

/**
 * @since 5.9.3
 */
public class CollectionAsynchrnonousQuery {

    public final static String QUERY_FOR_COLLECTION_REMOVED = "SELECT * FROM Document WHERE ecm:isProxy = 0 AND collectionMember:collectionIds/* = ?";

    public final static String QUERY_FOR_COLLECTION_MEMBER_REMOVED = "SELECT * FROM Document WHERE collection:documentIds/* = ?";

    public final static long MAX_RESULT = 50;
}
