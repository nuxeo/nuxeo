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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.remoting.transporter;

import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableUnMarshaller;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoUnMarshaller extends SerializableUnMarshaller {

    private static final long serialVersionUID = -4715053329441344482L;

    public NuxeoUnMarshaller() {
        // The only method I found to set nuxeo specific class loader
        customClassLoader = getClass().getClassLoader();
    }

    @Override
    public void setClassLoader(ClassLoader classloader) {
        // ignore - we are always using nuxeo class loader
    }

    @Override
    public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException {
        NuxeoUnMarshaller unmarshaller = new NuxeoUnMarshaller();
        unmarshaller.setClassLoader(customClassLoader);
        return unmarshaller;
    }

}
