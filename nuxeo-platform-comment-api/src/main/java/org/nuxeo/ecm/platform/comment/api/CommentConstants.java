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

package org.nuxeo.ecm.platform.comment.api;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public final class CommentConstants {

    public static final String EVENT_COMMENT_CATEGORY = "commentCategory";

    public static final String PARENT_COMMENT = "parentComment";

    // FIXME This should be changed to COMMENT_DOCUMENT = "comment_document"
    // (see NXP-2806)
    public static final String COMMENT = "comment";

    public static final String COMMENT_TEXT = "comment_text";

    private CommentConstants() {
    }

}
