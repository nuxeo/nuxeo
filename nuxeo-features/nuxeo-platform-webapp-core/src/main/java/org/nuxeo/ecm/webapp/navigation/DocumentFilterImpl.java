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

package org.nuxeo.ecm.webapp.navigation;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DocumentFilterImpl implements DocumentFilter, Serializable {

    private static final long serialVersionUID = 1L;

    protected final boolean showSection;

    protected final boolean showFiles;


    public DocumentFilterImpl(boolean showSection, boolean showFiles) {
        this.showSection = showSection;
        this.showFiles = showFiles;
    }

    public boolean accept(DocumentModel document) {
        String type = document.getType();
        boolean isFolder = document.isFolder();

        if (!showSection && type.equals("Section")) {
            return false;
        }

        //exclude deleted documents from tree
        try {
            if ("deleted".equals(document.getCurrentLifeCycleState())){
                return false;
            }
        } catch (ClientException e) {
            return false;
        }

        return showFiles || isFolder;
    }

}
