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

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.io.IOException;
import java.io.Writer;

import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializerHandler;
import org.nuxeo.ecm.platform.rendering.wiki.WikiWriter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WikiBlockWriter extends WikiWriter {

    protected final String blockName;

    public WikiBlockWriter(WikiWriter parent, String blockName) {
        super(parent);
        this.blockName = blockName;
    }

    @Override
    public void writeTo(WikiSerializerHandler handler, Writer writer) throws IOException {
        if (writer instanceof BlockWriter) {
            BlockWriter parentWriter = (BlockWriter) writer;
            @SuppressWarnings("resource") // BlockWriter chaining makes close() hazardous
            BlockWriter bw = new BlockWriter("__dynamic__wiki", blockName, parentWriter.getRegistry());
            boolean parentSuppressOutput = parentWriter.getSuppressOutput();
            try {
                parentWriter.setSuppressOutput(true);
                super.writeTo(handler, bw);
                parentWriter.writeBlock(bw);
            } finally {
                parentWriter.setSuppressOutput(parentSuppressOutput);
            }
        } else {
            throw new IllegalArgumentException("Unsupported target writer");
        }
    }

}
