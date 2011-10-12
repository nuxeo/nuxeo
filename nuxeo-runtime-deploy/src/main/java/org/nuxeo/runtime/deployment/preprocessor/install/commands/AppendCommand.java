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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.filters.IncludeFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AppendCommand implements Command {

    protected final Path src;

    protected final Path dst;

    protected final boolean addNewLine;
    
    protected final FileNamePattern pattern;

    public AppendCommand(Path src, Path dst, boolean addNewLine, FileNamePattern pattern) {
        this.src = src;
        this.dst = dst;
        this.addNewLine = addNewLine;
        this.pattern = pattern;
    }

    public AppendCommand(Path src, Path dst) {
        this(src, dst, true, null);
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File srcFile = new File(baseDir, ctx.expandVars(src.toString()));
        File dstFile = new File(baseDir, ctx.expandVars(dst.toString()));

        if (pattern == null && !srcFile.exists()) {
            throw new FileNotFoundException("Could not find the file "
                    + srcFile.getAbsolutePath() + " to append.");
        }

        if (!dstFile.isFile()) {
            try {
                dstFile.createNewFile();
            } catch (IOException e) {
                throw new IOException("Could not create " + dstFile, e);
            }
        }
        if (pattern == null) {
            FileUtils.append(srcFile, dstFile, addNewLine);
        } else {
            ArrayList<File> files = new ArrayList<File>();
            FileUtils.collectFiles(srcFile, pattern, files);
            for (File file:files) {
                FileUtils.append(file, dstFile);
            }
        }
    }

    @Override
    public String toString() {
        return "append " + src.toString() + " > " + dst.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "append " + ctx.expandVars(src.toString()) + " > "
                + ctx.expandVars(dst.toString());
    }

}
