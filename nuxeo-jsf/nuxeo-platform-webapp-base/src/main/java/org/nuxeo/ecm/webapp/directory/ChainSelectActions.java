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
 * $Id: ChainSelectActions.java 28950 2008-01-11 13:35:06Z tdelprat $
 */

package org.nuxeo.ecm.webapp.directory;

import javax.faces.event.ActionEvent;

/**
 * An Seam component that handles the add/remove actions on chain selects.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface ChainSelectActions {

    void add(ActionEvent event);

    void delete(ActionEvent event);

}
