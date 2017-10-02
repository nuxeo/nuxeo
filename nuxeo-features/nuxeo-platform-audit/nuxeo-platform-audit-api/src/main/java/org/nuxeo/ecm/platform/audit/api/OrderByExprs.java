/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;

/**
 * Helper to instantiate {@link OrderByExpr}s compatible with the search service inside Audit.
 * <p/>
 * This class is meant to be moved and to grow up in order to handle every cases.
 *
 * @since 9.3
 */
public class OrderByExprs {

    public static OrderByExpr asc(String name) {
        return new OrderByExpr(new Reference(name), false);
    }

    public static OrderByExpr desc(String name) {
        return new OrderByExpr(new Reference(name), true);
    }

}
