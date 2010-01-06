/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.chemistry.shell.console;

import java.io.File;
import java.util.List;

import jline.FileNameCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DirectoryCompletor extends FileNameCompletor {

    @Override
    public int matchFiles(String buffer, String translated, File[] entries,
            List candidates) {
        if (entries == null) {
            return -1;
        }

        for (File entry : entries) {
            if (entry.getAbsolutePath().startsWith(translated) && entry.isDirectory()) {
                candidates.add(entry.getName());
            }
        }

        int index = buffer.lastIndexOf(File.separator);

        return index + File.separator.length();
    }

}
