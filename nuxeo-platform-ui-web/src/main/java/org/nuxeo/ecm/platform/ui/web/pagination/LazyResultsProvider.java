/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.ui.web.pagination;

import org.nuxeo.ecm.core.api.PagedDocumentsProvider;

/**
 * Backwards compatibility after interface renaming.
 * Will be removed in Nuxeo 5.2
 *
 * @deprecated use PagedDocumentsProvider
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
@Deprecated
public interface LazyResultsProvider extends PagedDocumentsProvider {

}
