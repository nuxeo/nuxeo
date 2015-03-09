/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add permission available for current user on given {@link DocumentModel}'s as json attachment. Limit permission to
 * Read, Write and Everything.
 * </p>
 * <p>
 * Enable if parameter enrichers.document=permissions is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "permissions": [ "Read", "Write", "Everything" ]  <- depending on current user permission on document
 *   }
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BasePermissionsJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "permissions";

    private final List<String> availablePermissions = Arrays.asList("Read", "Write", "Everything");

    public BasePermissionsJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        jg.writeArrayFieldStart(NAME);
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            for (String permission : getPermissionsInSession(document, wrapper.getSession())) {
                jg.writeString(permission);
            }
        }
        jg.writeEndArray();
    }

    private Iterable<String> getPermissionsInSession(final DocumentModel doc, final CoreSession session) {
        final Principal principal = session.getPrincipal();
        return Iterables.filter(availablePermissions, new Predicate<String>() {
            @Override
            public boolean apply(String permission) {
                return session.hasPermission(principal, doc.getRef(), permission);
            }
        });
    }

}
