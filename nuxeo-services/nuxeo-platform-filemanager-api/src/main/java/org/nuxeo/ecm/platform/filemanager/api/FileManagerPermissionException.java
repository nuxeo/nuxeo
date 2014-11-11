/* (C) Copyright 2007 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JNDILocations.java 2992 2006-09-18 09:02:50Z janguenot $
 */
package org.nuxeo.ecm.platform.filemanager.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentSecurityException;

/**
 * Marker exception to be raised by a FileManagerService importer plugin to tell
 * the service that it could not create the requested document because of an
 * missing authorization.
 * <p>
 * The FileManagerService service is responsible to catch this exception and
 * build an informative message for the UI layer.
 * <p>
 * We derive from ClientException not to break existing API (BBB) though this is
 * not strictly required.
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 * @deprecated use the standard security exception {@link SecurityException} or
 *             {@link DocumentSecurityException}
 */
@Deprecated
public class FileManagerPermissionException extends ClientException {

    private static final long serialVersionUID = 376060003175460864L;

}
