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

package org.nuxeo.ecm.webapp.filemanager;

import javax.ejb.Local;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas Kalogeropoulos</a>
 *
 */
@Local
public interface FileManageWSLocal extends StatefulBaseLifeCycle {

    void initialize();

    void destroy();

    String display();

    String edit() throws ClientException;

    String editSelectedDocument() throws ClientException;

}
