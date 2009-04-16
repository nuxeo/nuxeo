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

package org.nuxeo.ecm.platform.rendering.wiki.extensions;

import java.io.IOException;
import java.io.Writer;

import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.nuxeo.ecm.platform.rendering.wiki.WikiSerializerHandler;
import org.nuxeo.ecm.platform.rendering.wiki.WikiWriter;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WikiBlockWriter extends WikiWriter {

    protected final String blockName;

    public WikiBlockWriter(WikiWriter parent, String blockName) {
        super(parent);
        this.blockName = blockName;
    }

    @Override
    public void writeTo(WikiSerializerHandler handler, Writer writer)
            throws IOException {
        if (writer instanceof BlockWriter) {
            BlockWriter parentWriter = (BlockWriter)writer;
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
