/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for Administration of Audit service.
 *
 * @author tiry
 */
public interface AuditAdmin {

    /**
     * Forces log Synchronisation for a branch of the repository. This can be
     * useful to add the create entries if DB was initialized from a bulk
     * import.
     */
    long syncLogCreationEntries(String repoId, String path, Boolean recurs)
            throws ClientException;
    
    Long getEventsCount(final String eventId);
}
