/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXMimeType.java 21336 2007-06-25 15:20:32Z janguenot $
 */

package org.nuxeo.ecm.platform.mimetype;

import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * NXMimeType helper.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class NXMimeType {

    private NXMimeType() {
    }

    public static MimetypeRegistryService getMimetypeRegistryService() {
        return (MimetypeRegistryService)
                Framework.getRuntime().getComponent(
                MimetypeRegistryService.NAME);
    }
}
