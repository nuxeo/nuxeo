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

package org.nuxeo.ecm.webapp.navigation;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

public class TypeBasedDocumentFilter extends DocumentFilterImpl {

    private static final long serialVersionUID = 176467356368353683L;

    private final List<String> filtredTypes;

    public TypeBasedDocumentFilter(boolean showSection, boolean showFiles,
            List<String> excludedTypes) {
        super(showSection, showFiles);
        filtredTypes = new ArrayList<String>();
        filtredTypes.addAll(excludedTypes);
    }

    @Override
    public boolean accept(DocumentModel docModel) {
        String docType = docModel.getType();
        if (filtredTypes.contains(docType)) {
            return false;
        }
        return super.accept(docModel);
    }

}
