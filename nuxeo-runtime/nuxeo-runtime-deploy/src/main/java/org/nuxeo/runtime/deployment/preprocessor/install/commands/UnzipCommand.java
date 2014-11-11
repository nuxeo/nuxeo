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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UnzipCommand implements Command {

    protected final Path src;
    protected final Path dst;
    protected final PathFilter filter;
    protected final String prefix;

    public UnzipCommand(Path src, Path dst) {
        this(src, dst, null,null);
    }

    public UnzipCommand(Path src, Path dst, PathFilter filter) {
        this(src, dst, filter,null);
    }

    public UnzipCommand(Path src, Path dst, String prefix) {
        this(src, dst, null ,prefix);
    }

    public UnzipCommand(Path src, Path dst, PathFilter filter, String prefix) {
        this.src = src;
        this.dst = dst;
        this.filter = filter;
        this.prefix=prefix;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        String srcPath = ctx.expandVars(src.toString());
		File srcFile = srcPath.startsWith("/") ? new File(srcPath) : new File(baseDir, srcPath);
		String dstPath = ctx.expandVars(dst.toString());
        File dstFile =  new File(baseDir, dstPath);

        if (!srcFile.exists()) {
            throw new FileNotFoundException("Could not find the file " + srcFile.getAbsolutePath() + " to unzip.");
        }

        if (srcFile.isDirectory()) {
            Path p = !StringUtils.isEmpty(prefix) ? new Path("/"+prefix) : new Path("/");
        	new CopyCommand(src.addTrailingSeparator(), dst.addTrailingSeparator(), p, filter).exec(ctx);
        	return;
        }

        if (dstFile.isFile()) {
            throw new IllegalArgumentException("When unziping the destination file must be a directory: "
                    + dstFile.getAbsolutePath());
        }

        if (!dstFile.exists()) {
            dstFile.mkdirs();
        }
        // unzip srcFile to directory dstFile
        if (filter != null) {
            if (prefix!=null) {
                ZipUtils.unzip(prefix,srcFile, dstFile, filter);
            } else {
                ZipUtils.unzip(srcFile, dstFile, filter);
            }
        } else {
            if (prefix!=null) {
                ZipUtils.unzip(prefix, srcFile, dstFile);
            } else {
                ZipUtils.unzip(srcFile, dstFile);
            }
        }
    }

    @Override
    public String toString() {
        return "unzip " + src.toString() + " > " + dst.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "unzip " + ctx.expandVars(src.toString()) + " > " +
                ctx.expandVars(dst.toString());
    }

}
