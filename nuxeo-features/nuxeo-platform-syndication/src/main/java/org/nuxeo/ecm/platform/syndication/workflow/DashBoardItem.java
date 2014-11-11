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
 * $Id: DashBoardItem.java 23683 2007-08-10 07:25:25Z btatar $
 */

package org.nuxeo.ecm.platform.syndication.workflow;

/**
 * Dashboard item.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Deprecated
public interface DashBoardItem extends
        org.nuxeo.ecm.platform.jbpm.dashboard.DashBoardItem {
    /**
     * REQUIRED FOR OPEN SOCIAL DASHBOARD
     */
    String getDocumentLink();

    void prependToComment(String setOfNames);

}
