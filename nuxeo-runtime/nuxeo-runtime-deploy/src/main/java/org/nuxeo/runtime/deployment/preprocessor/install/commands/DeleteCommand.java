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
public class DeleteCommand implements Command {

    protected final Path path;

    /**
     * Constructor for delete command.
     *
     * @param path the path relative to the root container.
     */
    public DeleteCommand(Path path) {
        this.path = path;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File file = new File(baseDir, ctx.expandVars(path.toString()));

        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            FileUtils.deleteTree(file);
        }
    }

    @Override
    public String toString() {
        return "delete " + path.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "delete " + ctx.expandVars(path.toString());
    }

}
