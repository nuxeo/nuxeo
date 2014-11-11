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
package org.nuxeo.build.swing;

import javax.swing.ImageIcon;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IconUtils {

    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(Class<?> clazz, String path,
                                               String description) {
        java.net.URL imgURL = clazz.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            //System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static ImageIcon createImageIcon(Class<?> clazz, String path) {
        return createImageIcon(clazz, path, null);
    }

}
