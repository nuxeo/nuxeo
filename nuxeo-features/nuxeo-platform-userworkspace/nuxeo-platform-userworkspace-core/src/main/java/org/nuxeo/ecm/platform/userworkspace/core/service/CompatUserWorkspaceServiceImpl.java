/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.userworkspace.core.service;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Alternate implementation that is backward compatible.
 * (Allow to have one UserWorkspace per user and per domain).
 *
 * @author Thierry Delprat
 */
public class CompatUserWorkspaceServiceImpl extends
        DefaultUserWorkspaceServiceImpl {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getDomainName(CoreSession userCoreSession, DocumentModel currentDocument) {
        if (currentDocument != null && currentDocument.getPath().segmentCount() > 0) {
            return currentDocument.getPath().segment(0);
        } else {
            return UserWorkspaceServiceImplComponent.getTargetDomainName();
        }
    }

}
