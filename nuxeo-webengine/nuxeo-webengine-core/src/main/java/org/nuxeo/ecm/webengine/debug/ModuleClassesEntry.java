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
package org.nuxeo.ecm.webengine.debug;

import java.io.File;

/**
 * This handles module packages (class directories).
 * Packages have valid java names and excludes skin and i18n.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleClassesEntry extends DirectoryEntry {

    public ModuleClassesEntry(File file) {
        super(file);
    }

    @Override
    protected void collectChildren() {
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (!"skin".equals(name) && !"i18n".equals(name) && name.indexOf('-') == -1) {
                    entries.add(new TreeEntry(f));
                }
            }
        }
    }

}
