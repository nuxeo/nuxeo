/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bjalon
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * Implementations of this interface manages the multi-tenant behavior for UserManager. This class will be used to fetch
 * the User Directory and the Group characteristics
 *
 * @author bjalon
 */
public interface UserMultiTenantManagement {

    /**
     * Transform filter and fulltext to fetch Groups for the given context and the query specified with the given filter
     * and fulltext. Be careful the filter map and the fulltext set object will be modified so copy them before.
     */
    void queryTransformer(UserManager um, Map<String, Serializable> filter, Set<String> fulltext, DocumentModel context);

    /**
     * Transform the Group DocumentModel store it into the tenant described by the context
     *
     * @param group to modified
     * @param context that bring the tenant information
     */
    DocumentModel groupTransformer(UserManager um, DocumentModel group, DocumentModel context);

    /**
     * Transforms the query builder to add tenant-related information.
     *
     * @return the transformed query builder
     * @since 10.3
     */
    QueryBuilder groupQueryTransformer(UserManager um, QueryBuilder queryBuilder, DocumentModel context);

    /**
     * Transform the GroupName to add to tenant characteristic.
     *
     * @param groupname to modified
     * @param context that bring the tenant information
     */
    String groupnameTranformer(UserManager um, String groupname, DocumentModel context);

}
