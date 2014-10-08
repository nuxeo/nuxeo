/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This enricher adds a list of the permissions granted to the user on the document
 *
 * @since 5.9.6
 */
public class UserPermissionsContentEnricher extends AbstractContentEnricher {

    private static final String PERMISSIONS_PARAMETER = "permissions";

    public static final String PERMISSIONS_CONTENT_ID = "permissions";

    private List<String> availablePermissions = new ArrayList<>();

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec)
            throws ClientException, IOException {
        final DocumentModel doc = ec.getDocumentModel();
        jg.writeStartArray();
        for (String permission : getPermissions(doc)) {
            jg.writeString(permission);
        }
        jg.writeEndArray();
        jg.flush();
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        String permissionsList = parameters.get(PERMISSIONS_PARAMETER);
        if (permissionsList != null) {
            availablePermissions.addAll(Arrays.asList(permissionsList.split(",")));
        }
    }

    private Iterable<String> getPermissions(final DocumentModel doc) {
        final CoreSession session = doc.getCoreSession();
        final Principal principal = session.getPrincipal();

        return Iterables.filter(availablePermissions, new Predicate<String>() {
            public boolean apply(String permission) {
                return session.hasPermission(principal, doc.getRef(), permission);
            }
        });
    }
}
