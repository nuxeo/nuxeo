/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.connect.update.PackageException;

/**
 * Copy a file to the given target directory or file. If the target is a
 * directory the file name is preserved. If the target file exists it will be
 * replaced if overwrite is true otherwise the command validation fails.
 * <p>
 * If md5 is set then the copy command will be validated only if the target file
 * has the same md5 as the one specified in the command.
 * <p>
 * The Copy command has as inverse either Delete either another Copy command. If
 * the file was copied without overwriting then Delete is the inverse (with a
 * md5 set to the one of the copied file). If the file was overwritten then the
 * Copy command has an inverse another copy command with the md5 to the one of
 * the copied file and the overwrite flag to true. The file to copy will be the
 * backup of the overwritten file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ParameterizedCopy extends Copy {

    @SuppressWarnings("hiding")
    public static final String ID = "pcopy";

    public ParameterizedCopy() {
        super(ID);
    }

    public ParameterizedCopy(File file, File tofile, String md5,
            boolean overwrite) {
        this();
        this.file = file;
        this.tofile = tofile;
        this.md5 = md5;
        this.overwrite = overwrite;
    }

    @Override
    protected String getContentToCopy(File fileToCopy, Map<String, String> prefs)
            throws PackageException {
        try {
            String content = FileUtils.readFile(fileToCopy);
            return StringUtils.expandVars(content, prefs);
        } catch (IOException e) {
            throw new PackageException("Failed to run parameterized copy for: "
                    + fileToCopy.getName(), e);
        }
    }

}
