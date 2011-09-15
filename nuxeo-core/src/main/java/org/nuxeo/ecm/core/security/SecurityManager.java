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

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SecurityManager {

    ACP getMergedACP(Document doc) throws SecurityException;

    ACP getACP(Document doc) throws SecurityException;

    void setACP(Document doc, ACP acp, boolean overwrite)
            throws SecurityException;

   /**
    * Checks whether this ACP grant the given permission on the given user.
    * <p>
    * The merged ACP is checked (this means all parents ACP + the local one)
    * but this doesn't check user groups or permission groups.
    * <p>
    * If the ACP is not explicitly denying or granting the permission false is returned
    * (the default behavior is to deny).
    *
    * @param doc the document
    * @param username the user name
    * @param permission the permission to check
    * @return true if granted, false if denied
    */
    boolean checkPermission(Document doc, String username,
            String permission) throws SecurityException;

    /**
     * Checks whether this ACP grant the given permission on the given user, denies it or
     * doesn't specify a rule.
     *
     * @param doc the document
     * @param username the user name
     * @param permission the permission to check
     * @return Access.GRANT if granted, Access.DENY if denied or Access.UNKNOWN if no rule for that permission exists.
     *         Never return null
     */
    Access getAccess(Document doc, String username,
            String permission) throws SecurityException;

}
