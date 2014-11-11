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
package org.nuxeo.ecm.collections.core.listener;

/**
 * @since 5.9.3
 */
public class CollectionAsynchrnonousQuery {

    public final static String QUERY_FOR_COLLECTION_REMOVED = "SELECT * FROM Document WHERE ecm:isProxy = 0 AND collectionMember:collectionIds/* = ?";

    public final static String QUERY_FOR_COLLECTION_MEMBER_REMOVED = "SELECT * FROM Document WHERE collection:documentIds/* = ?";

    public final static long MAX_RESULT = 50;
}
