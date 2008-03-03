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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.archive.api;

import java.io.Serializable;

public interface ArchiveRecordFactory extends Serializable {

    /**
     * This method returns the class this factory will instantiate.
     *
     * @return - an ArchiveRecord class definition.
     */
    Class<ArchiveRecord> getArchiveRecordClass();

    /**
     * This method is used to generate the archive record from the current
     * document model.
     *
     * @param currentDocument -the current document instance
     * @return an archive record instance populated with information from the
     *         current document model.
     * @throws Exception
     */
    ArchiveRecord generateArchiveRecordFrom(Object currentDocument)
            throws Exception;
}
