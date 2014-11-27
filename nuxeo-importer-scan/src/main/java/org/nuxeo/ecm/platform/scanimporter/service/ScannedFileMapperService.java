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
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.File;
import java.io.IOException;

/**
 *
 * Service interface
 *
 * @author Thierry Delprat
 *
 */
public interface ScannedFileMapperService {

    ScanFileBlobHolder parseMetaData(File xmlFile) throws IOException;

    String getTargetContainerType();

    ImporterConfig getImporterConfig();

    String getTargetLeafType();
}
