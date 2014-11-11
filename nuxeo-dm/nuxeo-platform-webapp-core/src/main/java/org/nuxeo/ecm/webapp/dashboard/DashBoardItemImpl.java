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
 * $Id: DashBoardItemImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.webapp.dashboard;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Dashboard item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * @deprecated use {@link org.nuxeo.ecm.platform.syndication.workflow.DashBoardItemImpl}
 *
 */
@Deprecated
public class DashBoardItemImpl extends org.nuxeo.ecm.platform.syndication.workflow.DashBoardItemImpl {

    public DashBoardItemImpl(TaskInstance task, DocumentModel document) {
        super(task, document);
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

}
