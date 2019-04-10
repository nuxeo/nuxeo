/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;

import org.dom4j.Document;

/**
 *
 * This interface may be used to have a custom logic for type mapping. This
 * allows to determine target DocumentModel type based on the descriptor file.
 *
 * @author Thierry Delprat
 *
 */
public interface DocumentTypeMapper {

    String getTargetDocumentType(Document xmlDoc, File file);

}
