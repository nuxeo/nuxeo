/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.operations;

import java.io.Serializable;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmOperation;
import org.nuxeo.ecm.platform.jbpm.NuxeoJbpmException;

/**
 * @author arussel
 *
 */
public class AddCommentOperation implements JbpmOperation {

    private static final long serialVersionUID = 1L;

    private final long id;

    private final String actorId;

    private final String message;

    public AddCommentOperation(long id, String actorId, String message) {
        this.id = id;
        this.actorId = actorId;
        this.message = message;
    }

    public Serializable run(JbpmContext context) throws NuxeoJbpmException {
        Comment comment = new Comment(actorId, message);
        TaskInstance ti = context.getTaskInstanceForUpdate(id);
        ti.addComment(comment);
        return null;
    }

}
