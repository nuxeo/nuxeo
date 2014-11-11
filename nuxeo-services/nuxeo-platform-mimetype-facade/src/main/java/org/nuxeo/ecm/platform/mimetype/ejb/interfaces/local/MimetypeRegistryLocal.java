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
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */

package org.nuxeo.ecm.platform.mimetype.ejb.interfaces.local;

import javax.ejb.Local;

import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.ExtensionDescriptor;

/**
 * MimetypeEntry registry local interface.
 *
 * @see MimetypeRegistry
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Local
public interface MimetypeRegistryLocal extends MimetypeRegistry {

    void registerMimetype(MimetypeEntry mimetype);

    void unregisterMimetype(String mimetype);

    void registerFileExtension(ExtensionDescriptor extension);

    void unregisterFileExtension(ExtensionDescriptor extension);

}
