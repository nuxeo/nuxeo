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

package org.nuxeo.ecm.platform.relations.search;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractResourceAdapter;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class FakeDocumentResourceAdapter extends AbstractResourceAdapter {

    private static class FakeDocumentModel extends DocumentModelImpl {

        private static final long serialVersionUID = 1L;

        private final String repoName;

        private final String id;

        FakeDocumentModel(String repoName, String id) {
            this.repoName = repoName;
            this.id = id;
        }

        @Override
        public DocumentRef getRef() {
            return new IdRef(id);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getRepositoryName() {
            return repoName;
        }
    }

    @Override
    public Object getResourceRepresentation(Resource resource) {
        return new FakeDocumentModel(Constants.REPOSITORY_NAME,
                ((QNameResource) resource).getLocalName());
    }

    @Override
    public Class<?> getKlass() {
        return DocumentModel.class;
    }

}
