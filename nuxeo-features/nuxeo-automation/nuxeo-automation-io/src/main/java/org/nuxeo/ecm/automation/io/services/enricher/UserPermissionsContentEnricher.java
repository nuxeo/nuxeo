/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BasePermissionsJsonEnricher;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * This enricher adds a list of the permissions granted to the user on the document
 *
 * @since 6.0
 * @deprecated This enricher was migrated to {@link BasePermissionsJsonEnricher}. The content enricher service doesn't
 *             work anymore.
 */
@Deprecated
public class UserPermissionsContentEnricher extends AbstractContentEnricher {

    private static final String PERMISSIONS_PARAMETER = "permissions";

    public static final String PERMISSIONS_CONTENT_ID = "permissions";

    private List<String> availablePermissions = new ArrayList<>();

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
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
            @Override
            public boolean apply(String permission) {
                return session.hasPermission(principal, doc.getRef(), permission);
            }
        });
    }
}
