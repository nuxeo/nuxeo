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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.util.Set;

/**
 * NXAuditEvents interface.
 * <p>
 * Allows to query for auditable events.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
// FIXME: (this interface only carries one method, and its deprecated, something must be done).
public interface NXAuditEvents extends Logs {

    /**
     * Returns the list of auditable event names.
     *
     * Is there any no reason to expose event names outside of audit service ? If
     * not, we will remove that API.
     *
     * @return list of String representing event names.
     */
    @Deprecated
    Set<String> getAuditableEventNames();

}
