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

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * Zip the content of a directory.
 * <p>
 * A prefix may be specified.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ZipCommand implements Command {

    protected final Path src;
    protected final Path dst;
    protected final PathFilter filter;
    protected final String prefix;


    public ZipCommand(Path src, Path dst) {
        this(src, dst, null, null);
    }

    public ZipCommand(Path src, Path dst, String prefix) {
        this(src, dst, prefix, null);
    }

    public ZipCommand(Path src, Path dst, String prefix, PathFilter filter) {
        this.src = src;
        this.dst = dst;
        this.prefix = prefix;
        this.filter = filter;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File srcFile = new File(baseDir, ctx.expandVars(src.toString()));
        File dstFile = new File(baseDir, ctx.expandVars(dst.toString()));

        if (!srcFile.exists()) {
            throw new FileNotFoundException("Could not find the file " + srcFile.getAbsolutePath() + " to zip.");
        }

        if (dstFile.isDirectory()) {
            throw new IllegalArgumentException("When ziping the destination file must be a file: "
                    + dstFile.getAbsolutePath());
        }

        File parent = dstFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (srcFile.isDirectory()) {
            // unzip only thre directory content
            // the prefix can be used to add the root directory itself
            File[] files = srcFile.listFiles();
            // zip files to directory dstFile
            if (filter != null) {
                ZipUtils.zip(files, dstFile, prefix);
                // TODO: add filter capabilities
            } else {
                ZipUtils.zip(files, dstFile, prefix);
            }
        } else {
            // zip srcFiles to directory dstFile
            if (filter != null) {
                ZipUtils.zip(srcFile, dstFile, prefix);
                // TODO: add filter capabilities
            } else {
                ZipUtils.zip(srcFile, dstFile, prefix);
            }
        }
    }

    @Override
    public String toString() {
        return "zip " + src.toString() + " > " + dst.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "zip " + ctx.expandVars(src.toString()) + " > " +
                ctx.expandVars(dst.toString());
    }

}
