/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.edit.vocabularies;

public final class VocabularyConstants {

    public static final String VOCABULARY_TYPE_SIMPLE = "vocabulary";

    public static final String VOCABULARY_TYPE_HIER = "xvocabulary";

    /**
     * @deprecated use {@link #VOCABULARY_TYPE_SIMPLE} or
     *             {@link #VOCABULARY_TYPE_HIER}
     */
    @Deprecated
    public static final String[] VOCABULARY_TYPES = { VOCABULARY_TYPE_SIMPLE,
            VOCABULARY_TYPE_HIER };

    public static final Integer DEFAULT_OBSOLETE = 0;

    public static final Integer DEFAULT_VOCABULARY_ORDER = 10000000;

    public static final String VOCABULARY_ID = "id";

    public static final String VOCABULARY_LABEL = "label";

    public static final String VOCABULARY_OBSOLETE = "obsolete";

    public static final String VOCABULARY_PARENT = "parent";

    public static final String VOCABULARY_ORDERING = "ordering";

    public static final String ID_COLUMN_SEARCH = "id";

    public static final String LABEL_COLUMN_SEARCH = "label";

    public static final String PARENT_COLUMN_SEARCH = "parent";

    public static final String ORDER_ASC = "asc";

    public static final String COMMAND_ADD = "command.add";

    public static final String COMMAND_EDIT = "command.edit";

    public static final String COMMAND_CANCEL = "command.cancel";

    private VocabularyConstants() {
    }

}
