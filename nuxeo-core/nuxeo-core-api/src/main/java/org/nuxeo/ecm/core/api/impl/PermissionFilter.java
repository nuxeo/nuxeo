/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on permissions.
 * <p>
 * If one of the permission check throws an Exception, the {@link #accept} method returns false.
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
        return session != null && hasPermission(session, docModel, excluded, false)
                && hasPermission(session, docModel, required, true);

    }

    protected boolean hasPermission(CoreSession session, DocumentModel doc, Set<String> permissions, boolean required) {
        for (String permission : permissions) {
            if ((required && !session.hasPermission(doc.getRef(), permission))
                    || (!required && session.hasPermission(doc.getRef(), permission))) {
                return false;
            }
        }
        return true;
    }

}
