/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.IOException;

import org.artofsolving.jodconverter.OfficeDocumentConverter;

/**
 * OOoManagerService can either start or stop OpenOffice pool server and return
 * an OfficeDocumentConverter.
 *
 * @author Laurent Doguin
 */
public interface OOoManagerService {

    OfficeDocumentConverter getDocumentConverter();

    void stopOOoManager();

    void startOOoManager() throws IOException;

    boolean isOOoManagerStarted();

}
