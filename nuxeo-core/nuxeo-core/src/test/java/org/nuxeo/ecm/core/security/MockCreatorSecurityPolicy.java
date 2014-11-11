/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;

/**
 * Security policy used for tests that grants every permission to the author of
 * the document.
 *
 * @author Anahide Tchertchian
 *
 */
public class MockCreatorSecurityPolicy extends AbstractSecurityPolicy {

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        String creator = null;
        if (doc != null) {
            try {
                creator = (String) doc.getPropertyValue("dc:creator");
            } catch (DocumentException e) {
            }
        }
        if (creator != null && creator.equals(principal.getName())) {
            access = Access.GRANT;
        }
        return access;
    }

}
