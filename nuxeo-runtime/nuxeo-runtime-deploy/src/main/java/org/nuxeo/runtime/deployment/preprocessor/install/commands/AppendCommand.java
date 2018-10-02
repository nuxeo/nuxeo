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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.FileNamePattern;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
            throw new FileNotFoundException("Could not find the file " + srcFile.getAbsolutePath()
                    + " to append to ' when deploying bundle '" + ctx.get("bundle") + "'.");
        }

        if (!dstFile.isFile()) {
            try {
                File parent = dstFile.getParentFile();
                if (!parent.isDirectory()) {
                    parent.mkdirs();
                }
                Files.createFile(dstFile.toPath());
            } catch (IOException e) {
                throw new IOException(
                        "Could not create '" + dstFile + "' when deploying bundle '" + ctx.get("bundle") + "'.", e);
            }
        }
        if (pattern == null) {
            append(srcFile, dstFile, addNewLine);
        } else {
            ArrayList<File> files = new ArrayList<File>();
            FileUtils.collectFiles(srcFile, pattern, files);
            for (File file : files) {
                append(file, dstFile, false);
            }
        }
    }

    @Override
    public String toString() {
        return "append " + src.toString() + " > " + dst.toString();
    }

    @Override
    public String toString(CommandContext ctx) {
        return "append " + ctx.expandVars(src.toString()) + " > " + ctx.expandVars(dst.toString());
    }

    private void append(File srcFile, File dstFile, boolean appendNewLine) throws IOException {
        String srcExt = FileUtils.getFileExtension(srcFile.getName());
        String dstExt = FileUtils.getFileExtension(dstFile.getName());
        boolean isDstEmpty = dstFile.length() == 0; // file empty or doesn't exists
        if (!isDstEmpty && StringUtils.equalsIgnoreCase(srcExt, dstExt) && "json".equalsIgnoreCase(srcExt)) {
            // merge the json
            ObjectMapper m = new ObjectMapper();
            ObjectNode destNode = m.readValue(dstFile, ObjectNode.class);
            ObjectNode srcNode = m.readValue(srcFile, ObjectNode.class);
            destNode.setAll(srcNode);
            m.writeValue(dstFile, destNode);
        } else {
            try (InputStream in = new FileInputStream(srcFile);
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(dstFile, true))) {
                if (appendNewLine) {
                    out.write(System.getProperty("line.separator").getBytes());
                }
                IOUtils.copy(in, out);
            }
        }
    }
}
