/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.elasticsearch;

public class ElasticSearchConstants {

    private ElasticSearchConstants() {
    }

    /**
     * Elasticsearch type name used to index Nuxeo documents
     */
    public static final String DOC_TYPE = "doc";

    public static final String ACL_FIELD = "ecm:acl";

    public static final String CHILDREN_FIELD = "ecm:path.children";

}
