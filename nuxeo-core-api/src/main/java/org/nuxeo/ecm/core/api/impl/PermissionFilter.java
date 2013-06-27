/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on permissions.
 * <p>
 * If one of the permission check throws an Exception, the {@link #accept}
 * method returns false.
 *
 * @since 5.7.2
 */
public class PermissionFilter implements Filter {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PermissionFilter.class);

    protected final Set<String> required;

    protected final Set<String> excluded;

    public PermissionFilter(List<String> required, List<String> excluded) {
        if (required == null) {
            this.required = Collections.emptySet();
        } else {
            this.required = new HashSet<>(required);
        }
        if (excluded == null) {
            this.excluded = Collections.emptySet();
        } else {
            this.excluded = new HashSet<>(excluded);
        }
    }

    public PermissionFilter(String permission, boolean isRequired) {
        if (isRequired) {
            required = Collections.singleton(permission);
            excluded = Collections.emptySet();
        } else {
            required = Collections.emptySet();
            excluded = Collections.singleton(permission);
        }
    }

    @Override
    public boolean accept(DocumentModel docModel) {
        CoreSession session = docModel.getCoreSession();
        return session != null
                && hasPermission(session, docModel, excluded, false)
                && hasPermission(session, docModel, required, true);

    }

    protected boolean hasPermission(CoreSession session, DocumentModel doc,
            Set<String> permissions, boolean required) {
        for (String permission : permissions) {
            try {
                if ((required && !session.hasPermission(doc.getRef(),
                        permission))
                        || (!required && session.hasPermission(doc.getRef(),
                                permission))) {
                    return false;
                }
            } catch (ClientException e) {
                String message = String.format(
                        "Unable to check '%s' permission for document '%s': %s",
                        permission, doc.getPathAsString(), e.getMessage());
                log.warn(message);
                log.debug(message, e);
                return false;
            }
        }
        return true;
    }

}
