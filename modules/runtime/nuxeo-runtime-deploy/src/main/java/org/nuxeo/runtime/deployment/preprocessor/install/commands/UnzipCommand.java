/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class UnzipCommand implements Command {

    protected final Path src;

    protected final Path dst;

    protected final PathFilter filter;

    protected final String prefix;

    public UnzipCommand(Path src, Path dst) {
        this(src, dst, null, null);
    }

    public UnzipCommand(Path src, Path dst, PathFilter filter) {
        this(src, dst, filter, null);
    }

    public UnzipCommand(Path src, Path dst, String prefix) {
        this(src, dst, null, prefix);
    }

    public UnzipCommand(Path src, Path dst, PathFilter filter, String prefix) {
        this.src = src;
        this.dst = dst;
        this.filter = filter;
        this.prefix = prefix;
    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        File baseDir = ctx.getBaseDir();
        File srcFile = new File(baseDir, ctx.expandVars(src.toString()));
        File dstFile = new File(baseDir, ctx.expandVars(dst.toString()));

        if (!srcFile.exists()) {
            throw new FileNotFoundException("Could not find the file " + srcFile.getAbsolutePath() + " to unzip.");
        }

        if (srcFile.isDirectory()) {
            Path p = StringUtils.isNotEmpty(prefix) ? new Path("/" + prefix) : new Path("/");
            new CopyCommand(src.addTrailingSeparator(), dst.addTrailingSeparator(), p, filter).exec(ctx);
            return;
        }

        if (dstFile.isFile()) {
            throw new IllegalArgumentException(
                    "When unziping the destination file must be a directory: " + dstFile.getAbsolutePath());
        }

        if (!dstFile.exists()) {
            dstFile.mkdirs();
        }
        // unzip srcFile to directory dstFile
        if (filter != null) {
            if (prefix != null) {
                ZipUtils.unzip(prefix, srcFile, dstFile, filter);
            } else {
                ZipUtils.unzip(srcFile, dstFile, filter);
            }
        } else {
            if (prefix != null) {
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
        return "unzip " + ctx.expandVars(src.toString()) + " > " + ctx.expandVars(dst.toString());
    }

}
