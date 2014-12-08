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
 */
package org.nuxeo.ecm.diff.content.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;

/**
 * Interface that should be implemented by contributed code to manage custom content diff adapters.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public interface ContentDiffAdapterFactory {

    ContentDiffAdapter getAdapter(DocumentModel doc);

}
