/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.routing.test;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.helper.ActionableValidator;

/**
 * A Test Helper class that simulate persistence of Step information. This
 * persistence is transient to the JVM.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class WaitingStepRuntimePersister {
    protected static final List<String> ids = new ArrayList<String>();

    static public void addStepId(String id) {
        if (ids.contains(id)) {
            throw new RuntimeException("Asking twice to wait on the same step.");
        }
        ids.add(id);
    }

    static public List<String> getStepIds() {
        return ids;
    }

    static public void resumeStep(final String id, CoreSession session) {
        if (!ids.contains(id)) {
            throw new RuntimeException("Asking to resume a non peristed step.");
        }
        new ActionableValidator(new ActionableObject() {
            @Override
            public String getValidateOperationChainId() {
                return "simpleValidate";
            }

            @Override
            public String getRefuseOperationChainId() {
                return "simpleRefuse";
            }

            @Override
            public DocumentRouteStep getDocumentRouteStep(CoreSession session) {
                try {
                    return session.getDocument(new IdRef(id)).getAdapter(
                            DocumentRouteStep.class);
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public DocumentModelList getAttachedDocuments(CoreSession session) {
                return new DocumentModelListImpl();
            }
        }, session).validate();
        ids.remove(id);
    }
}
