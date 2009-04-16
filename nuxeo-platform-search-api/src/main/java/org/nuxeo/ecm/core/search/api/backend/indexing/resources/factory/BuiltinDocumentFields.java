/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory;

/**
 * Builtin document fields constants.
 *
 * <p>
 * Those constants are used:
 * <ul>
 * <li> for data transmission between search service and backend (the latter
 * might need to apply specific handling on them)
 * <li> as tokens in NXQL WHERE clauses
 * </ul>
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class BuiltinDocumentFields {

    public static final String DOC_BUILTINS_RESOURCE_NAME = "ecm";

    public static final String DOC_BUILTIN_PREFIX = "ecm";

    // Remove prefixed notations here. It should be taken from the builtins
    // indexable resource type.

    public static final String FIELD_DOC_QID = "ecm:qid";

    public static final String FIELD_DOC_REF = "ecm:id";

    public static final String FIELD_DOC_UUID = "ecm:uuid";

    public static final String FIELD_DOC_NAME = "ecm:name";

    public static final String FIELD_DOC_PARENT_REF = "ecm:parentId";

    public static final String FIELD_DOC_TYPE = "ecm:primaryType";

    public static final String FIELD_FULLTEXT = "ecm:fulltext";

    public static final String FIELD_DOC_FACETS = "ecm:mixinType";

    public static final String FIELD_DOC_PATH = "ecm:path";

    public static final String FIELD_DOC_URL = "ecm:url";

    public static final String FIELD_DOC_LIFE_CYCLE = "ecm:currentLifeCycleState";

    public static final String FIELD_DOC_VERSION_LABEL = "ecm:versionLabel";

    public static final String FIELD_DOC_IS_CHECKED_IN_VERSION = "ecm:isCheckedInVersion";

    public static final String FIELD_DOC_IS_PROXY = "ecm:isProxy";

    public static final String FIELD_DOC_REPOSITORY_NAME = "ecm:repositoryName";

    public static final String FIELD_DOC_FLAGS = "ecm:flags";

    // fields that shouldn't end up in WHERE clauses

    public static final String FIELD_ACP_INDEXED = "builtin_acp_indexed";

    public static final String FIELD_ACP_STORED = "builtin_acp_stored";

    // Utility class.
    private BuiltinDocumentFields() {
    }

    public static String getPrefixedNameFor(String name) {
        if (!name.contains(":")) {
            // XXX ensure in the list.
            return DOC_BUILTIN_PREFIX + ':' + name;
        }
        return name;
    }

}
