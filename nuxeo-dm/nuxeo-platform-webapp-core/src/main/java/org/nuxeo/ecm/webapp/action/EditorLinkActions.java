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
 *     Thomas Roger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.action;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface EditorLinkActions {

    String searchDocuments() throws ClientException;

    List<DocumentModel> getSearchDocumentResults();

    boolean getHasSearchResults();

    String getSearchKeywords();

    void setSearchKeywords(final String searchKeywords);

    void destroy();

}
