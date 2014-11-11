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
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
            FileUtils.writeFile(file, content);
        } else {
            file.createNewFile();
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
