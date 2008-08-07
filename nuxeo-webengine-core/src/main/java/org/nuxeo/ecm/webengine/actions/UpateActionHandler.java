/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.actions;


import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.ecm.webengine.forms.FormData;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class UpateActionHandler implements ActionHandler {

    public void run(WebObject object) throws WebException {
        WebContext context = object.getWebContext();
        DocumentModel doc = object.getDocument();
        try {
            FormData form = context.getForm();
            form.fillDocument(doc);
            VersioningActions va = form.getVersioningOption();
            if (va != null) {
                ScopedMap ctxData = doc.getContextData();
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
                ctxData.putScopedValue(ScopeType.REQUEST, VersioningActions.KEY_FOR_INC_OPTION, va);
            } else {
                ScopedMap ctxData = doc.getContextData();
                ctxData.putScopedValue(ScopeType.REQUEST,
                        VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, false);
            }
            doc = context.getCoreSession().saveDocument(doc);
            context.getCoreSession().save();
        } catch (Exception e) {
            throw new WebException("Failed to update document", e);
        }
    }

}
