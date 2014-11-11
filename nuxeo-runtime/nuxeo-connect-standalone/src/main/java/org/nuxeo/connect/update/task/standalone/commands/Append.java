/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;

/**
 * Append content of a file into a destination file.
 * <p>
 * Rollback command is a Copy of the original file.
 */
public class Append extends Copy {

    @SuppressWarnings("hiding")
    public static final String ID = "append";

    public Append() {
        super(ID);
        overwrite = true;
        append = true;
    }

    public Append(File file, File tofile) {
        super(ID, file, tofile, null, true, false);
        append = true;
    }

}
