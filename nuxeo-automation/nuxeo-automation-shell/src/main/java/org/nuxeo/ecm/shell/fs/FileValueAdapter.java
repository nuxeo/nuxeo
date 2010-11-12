/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.fs;

import java.io.File;

import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ValueAdapter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class FileValueAdapter implements ValueAdapter {

    @SuppressWarnings("unchecked")
    public <T> T getValue(Shell shell, Class<T> type, String value) {
        if (type == File.class) {
            if (value.startsWith("/")) {
                return (T) new File(value);
            } else {
                FileSystem fs = shell.getContextObject(FileSystem.class);
                if (fs != null) {
                    return (T) new File(fs.pwd(), value);
                }
            }
        }
        return null;
    }

}
