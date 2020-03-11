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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MoveCommand implements Command {

    protected final Path src;

    protected final Path dst;

    protected final PathFilter filter;

    /**
     * Constructor for copy command.
     *
     * @param src the path relative to the root container. The path will be made absolute if not already
     * @param dst the path relative to teh root container of the destination. If it is ending with a slash '/' the
     *            destination path is treated as a directory
     */
    public MoveCommand(Path src, Path dst) {
        this(src, dst, null);
    }

    public MoveCommand(Path src, Path dst, PathFilter filter) {
        this.src = src;
        this.dst = dst;
        this.filter = filter;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File srcFile = new File(baseDir, ctx.expandVars(src.toString()));
        File dstFile = new File(baseDir, ctx.expandVars(dst.toString()));

        if (!srcFile.exists()) {
            throw new FileNotFoundException("Could not find the file " + srcFile.getAbsolutePath() + " to move.");
        }

        if (filter != null && !filter.accept(new Path(dstFile.getAbsolutePath()))) {
            return;
        }

        if (!dstFile.exists()) {
            if (dst.hasTrailingSeparator()) {
                dstFile.mkdirs();
            } else {
                // make sure parent dirs exists
                File parent = dstFile.getParentFile();
                if (!parent.isDirectory()) {
                    parent.mkdirs();
                }
            }
        }

        if (dstFile.exists()) {
            Files.delete(dstFile.toPath());
        }

        Files.move(srcFile.toPath(), dstFile.toPath());
    }

    @Override
    public String toString() {
        return "copy " + src.toString() + " > " + dst.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "copy " + ctx.expandVars(src.toString()) + " > " + ctx.expandVars(dst.toString());
    }

}
