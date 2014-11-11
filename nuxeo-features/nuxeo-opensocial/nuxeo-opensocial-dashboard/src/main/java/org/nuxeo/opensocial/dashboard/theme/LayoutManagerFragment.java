/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.dashboard.theme;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;

public class LayoutManagerFragment extends AbstractFragment {

    @Override
    public Model getModel() throws ModelException {
        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        if (documentManager != null) {
            return getModel(documentManager);

        }
        return null;
    }

    public Model getModel(CoreSession session) throws ModelException {

        LayoutManagerModel model = new LayoutManagerModel();

        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        model.setAnonymous(principal.isAnonymous());

        return model;
    }

}
