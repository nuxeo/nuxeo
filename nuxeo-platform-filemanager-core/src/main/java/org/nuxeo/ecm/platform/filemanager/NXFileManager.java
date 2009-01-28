/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */

package org.nuxeo.ecm.platform.filemanager;

import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.runtime.api.Framework;

/**
 * NXFileManager helper.
 *
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         kalogeropoulos</a>
 *
 */
public final class NXFileManager {

    private NXFileManager() {
    }

    public static FileManagerService getFileManagerService() {
        return (FileManagerService) Framework.getRuntime().getComponent(
                FileManagerService.NAME);
    }

}
