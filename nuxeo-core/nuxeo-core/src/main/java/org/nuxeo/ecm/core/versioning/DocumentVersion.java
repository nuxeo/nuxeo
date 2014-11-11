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

package org.nuxeo.ecm.core.versioning;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;

/**
 * @author <a href="mailto:eionica@nuxeo.com">Eugen Ionica</a>
 *
 */

public interface DocumentVersion extends Document {

    /**
     * @return date and time when version is created
     * @throws DocumentException
     */
    Calendar getCreated() throws DocumentException;

    String getLabel() throws DocumentException;

    String getDescription() throws DocumentException;

    // public void setLabel(String label) throws DocumentException;
    // public void setDescription(String description) throws DocumentException;

    /**
     * @return immediate predecessors of this version in the version history
     * @throws DocumentException
     */
    DocumentVersion[] getPredecessors() throws DocumentException;

    /**
     * @return immediate successors of this version in the version history
     * @throws DocumentException
     */
    DocumentVersion[] getSuccessors() throws DocumentException;

}
