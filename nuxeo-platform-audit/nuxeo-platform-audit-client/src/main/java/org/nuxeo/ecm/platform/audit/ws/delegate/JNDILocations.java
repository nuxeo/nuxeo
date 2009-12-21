/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: JNDILocations.java 17822 2007-04-26 04:46:23Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ws.delegate;

/**
 * Holds JNDI locations of the EJBs.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class JNDILocations {

    public static final String nxauditWsAuditLocalLocation = "nuxeo/WSAuditBean/local";
    public static final String nxauditWsAuditRemoteLocation = "nuxeo/WSAuditBean/remote";

    private JNDILocations() {
    }

}
