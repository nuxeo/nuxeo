/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.tag;

/**
 * The tag constants.
 *
 * @author rux
 */
public class TagConstants {

    /** Root Tag directory title */
    public static final String TAGS_DIRECTORY = "Tags";

    /** For the moment, the Root Tag is hidden. */
    public static final String HIDDEN_FOLDER_TYPE = "HiddenFolder";

    /** Query the tags in a group */
    public static final String TAGS_IN_DOMAIN_QUERY_TEMPLATE =
        "SELECT * FROM Tag WHERE ecm:path STARTSWITH '%s' AND (tag:private = 0 or dc:creator = '%s')";
    public static final String DOCUMENTS_IN_DOMAIN_QUERY_TEMPLATE =
        "SELECT * FROM Document WHERE ecm:path STARTSWITH '%s'";

    /** The "is private" property of the tag schema */
    public static final String TAG_IS_PRIVATE_FIELD = "tag:private";

    /** The "label" property of the tag schema */
    public static final String TAG_LABEL_FIELD = "tag:label";

    public static final String TAG_DOCUMENT_TYPE = "Tag";

}
