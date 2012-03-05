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
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;
import java.io.FilenameFilter;

/**
 * {@link FilenameFilter} implemetation to skip files that are not descriptors
 *
 * @author Thierry Delprat
 *
 */
public class XmlMetaDataFileFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
        if (name.endsWith(".xml")) {
            return true;
        }
        return false;
    }

}
