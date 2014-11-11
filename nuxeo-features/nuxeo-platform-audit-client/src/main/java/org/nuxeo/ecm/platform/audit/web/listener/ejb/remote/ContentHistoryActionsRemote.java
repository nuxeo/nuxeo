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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.web.listener.ejb.remote;

import javax.ejb.Remote;

import org.nuxeo.ecm.platform.audit.web.listener.ContentHistoryActions;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Remote
public interface ContentHistoryActionsRemote extends ContentHistoryActions {

}
