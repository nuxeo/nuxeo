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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import freemarker.template.TemplateException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BlockWriter extends Writer {

    BlockWriterRegistry reg;

    final String page;

    final String name;

    StringBuilder sb = new StringBuilder();

    List<String> segments = new ArrayList<>();

    List<String> blocks = new ArrayList<>();

    BlockWriter superBlock; // the direct parent in the hierarchy - null if none

    BlockWriter baseBlock; // the root of the block hierarchy - null if none

    String ifBlockDefined;

    // used to avoid writing blocks or characters in the current block writer.
    // This is the case of the extension tag - that should ignore any content
    // and blocks too because blocks inside extension tag
    // must be derived blocks (the base hierarchy block will be found later when
    // the extended base template will be parsed)
    boolean suppressOutput = false;

    public BlockWriter(String page, String name, BlockWriterRegistry reg) {
        this.reg = reg;
        this.name = name;
        this.page = page;
    }

    public final BlockWriterRegistry getRegistry() {
        return reg;
    }

    public void setSuppressOutput(boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
    }

    public boolean getSuppressOutput() {
        return suppressOutput;
    }

    @Override
    public void close() throws IOException {
        sb = null;
        segments = null;
        blocks = null;
        superBlock = null;
        reg = null;
    }

    @Override
    public void flush() throws IOException {
        // do nothing
    }

    public boolean isEmpty() {
        return sb.length() == 0 && segments.isEmpty() && blocks.isEmpty();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (!suppressOutput) {
            sb.append(cbuf, off, len);
        }
    }

    public void writeBlock(BlockWriter bw) {
        if (!suppressOutput) {
            // add the current builder to the segments list
            segments.add(sb.toString());
            // reset builder
            sb.setLength(0);
            // ad the sub block to the children block list
            blocks.add(bw.name);
        }
        // inform the container about the new block
        reg.addBlock(bw.name, bw);
    }

    public void writeSuperBlock() {
        if (!suppressOutput) {
            segments.add(sb.toString()); // add the current builder to the
            // segments list
            sb.setLength(0); // reset builder
            blocks.add(".."); // add a special key that represent the super
            // block
        }
    }

    @SuppressWarnings("resource")
    public void copyTo(Writer writer) throws TemplateException, IOException {
        // check first if you need to suppress this block
        if (ifBlockDefined != null) {
            BlockWriter bw = reg.getBlock(ifBlockDefined);
            if (bw == null || bw.isEmpty()) {
                return;
            }
        }
        for (int i = 0, len = segments.size(); i < len; i++) {
            writer.write(segments.get(i));
            String key = blocks.get(i);
            BlockWriter bw = null;
            if (key == "..") { // the super block
                bw = superBlock;
            } else { // a regular block
                bw = reg.getBlock(key);
            }
            bw.copyTo(writer);
        }
        writer.write(sb.toString());
    }

    @Override
    public String toString() {
        return name + "@" + page;
    }
}
