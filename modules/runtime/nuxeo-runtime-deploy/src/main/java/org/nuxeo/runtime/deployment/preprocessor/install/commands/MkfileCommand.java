/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MkfileCommand implements Command {

    protected final Path path;

    protected final byte[] content;

    /**
     * Constructor for mkfile command.
     *
     * @param path the path relative to the root container
     * @param content the file content as an array of bytes
     */
    public MkfileCommand(Path path, byte[] content) {
        this.path = path;
        this.content = content;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File file = new File(baseDir, ctx.expandVars(path.toString()));

        File parent = file.getParentFile();
        if (!parent.isDirectory()) {
            // make sure the parent exists
            parent.mkdirs();
        }

        if (content != null && content.length > 0) {
            FileUtils.writeByteArrayToFile(file, content);
        } else {
            Files.createFile(file.toPath());
        }
    }

    @Override
    public String toString() {
        return "mkfile " + path.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "mkfile " + ctx.expandVars(path.toString());
    }

}
