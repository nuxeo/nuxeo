/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
public class BlockWriter extends Writer {

    BlockWriterRegistry reg;

    final String page;
    final String name;
    StringBuilder buf = new StringBuilder();
    List<String> segments = new ArrayList<String>();
    List<String> blocks = new ArrayList<String>();
    BlockWriter superBlock;

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
        buf = null;
        segments = null;
        blocks = null;
        superBlock = null;
        reg = null;
    }

    @Override
    public void flush() throws IOException {
        // do nothing
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (!suppressOutput) {
            buf.append(cbuf, off, len);
        }
    }

    public void writeBlock(BlockWriter bw) throws IOException {
        if (!suppressOutput) {
            segments.add(buf.toString()); // add the precedent buffer to the segments list
            buf.setLength(0); // reset buffer
            blocks.add(bw.name); // ad the sub block to the children block list
        }
        reg.addBlock(bw.name, bw); // inform the container about the new block
    }

    //TODO - is not working for now
    public void writeSuperBlock() throws IOException {
//        segments.add(buf.toString()); // add the precedent buffer to the segments list
//        buf.setLength(0); // reset buffer
//        String name = new StringBuilder(64).append(this.name).append('#').append(reg.level+1).toString();
//        blocks.add(name); // add the sub block to the children block list
    }

    public void copyTo(Writer writer) throws TemplateException, IOException {
        for (int i=0, len=segments.size(); i<len; i++) {
            writer.write(segments.get(i));
            reg.getBlock(blocks.get(i)).copyTo(writer);
        }
        writer.write(buf.toString());
    }

    @Override
    public String toString() {
        return name;
    }
}
