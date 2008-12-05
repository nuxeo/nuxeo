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

package org.nuxeo.ecm.webengine.fake;

import java.util.HashSet;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

public class FakeFolderishDocumentModel extends DocumentModelImpl {

    private static final long serialVersionUID = 1L;

    public FakeFolderishDocumentModel(String parentPath, String name, String type) {
        super(parentPath, name, type);
        declaredFacets = new HashSet<String>();
        declaredFacets.add("Folderish");
    }

}
