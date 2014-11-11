/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 */

package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 5.5
 */
public class TaskComment extends HashMap<String, Serializable> {

    private static final long serialVersionUID = 1L;

    public TaskComment(Map<String, Serializable> taskCommentMap) {
        super();
        this.putAll(taskCommentMap);
    }

    public TaskComment(String author, String text) {
        super();
        this.put(TaskConstants.TASK_COMMENT_AUTHOR_KEY, author);
        this.put(TaskConstants.TASK_COMMENT_TEXT_KEY, text);
        this.put(TaskConstants.TASK_COMMENT_CREATION_DATE_KEY, new Date());
    }

    public TaskComment(String author, String text, Date commentDate) {
        super();
        this.put(TaskConstants.TASK_COMMENT_AUTHOR_KEY, author);
        this.put(TaskConstants.TASK_COMMENT_TEXT_KEY, text);
        this.put(TaskConstants.TASK_COMMENT_CREATION_DATE_KEY, commentDate);
    }

    public String getAuthor() {
        return (String) this.get(TaskConstants.TASK_COMMENT_AUTHOR_KEY);
    }

    public String getText() {
        return (String) this.get(TaskConstants.TASK_COMMENT_TEXT_KEY);
    }

    public Date getCreationDate() {
        return (Date) this.get(TaskConstants.TASK_COMMENT_CREATION_DATE_KEY);
    }

}
