/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.connect.update.PackageException;

/**
 * Copy a file to the given target directory or file. If the target is a directory the file name is preserved. If the
 * target file exists it will be replaced if overwrite is true otherwise the command validation fails.
 * <p>
 * If md5 is set then the copy command will be validated only if the target file has the same md5 as the one specified
 * in the command.
 * <p>
 * The Copy command has as inverse either Delete either another Copy command. If the file was copied without overwriting
 * then Delete is the inverse (with a md5 set to the one of the copied file). If the file was overwritten then the Copy
 * command has an inverse another copy command with the md5 to the one of the copied file and the overwrite flag to
 * true. The file to copy will be the backup of the overwritten file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ParameterizedCopy extends Copy {

    @SuppressWarnings("hiding")
    public static final String ID = "pcopy";

    public ParameterizedCopy() {
        super(ID);
    }

    public ParameterizedCopy(File file, File tofile, String md5, boolean overwrite) {
        this();
        this.file = file;
        this.tofile = tofile;
        this.md5 = md5;
        this.overwrite = overwrite;
    }

    @Override
    protected String getContentToCopy(File fileToCopy, Map<String, String> prefs) throws PackageException {
        try {
            String content = FileUtils.readFileToString(fileToCopy);
            return StringUtils.expandVars(content, prefs);
        } catch (IOException e) {
            throw new PackageException("Failed to run parameterized copy for: " + fileToCopy.getName(), e);
        }
    }

}
