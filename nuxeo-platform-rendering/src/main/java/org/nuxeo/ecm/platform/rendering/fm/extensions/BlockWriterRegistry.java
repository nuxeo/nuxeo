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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlockWriterRegistry {

    protected final Map<String, BlockWriter> blockMap = new HashMap<String, BlockWriter>();

    public void addBlock(String name, BlockWriter bw) {
        BlockWriter existingBw = blockMap.get(name);
        if (existingBw != null) {
            // get the base block and set its superBlock
            if (existingBw.baseBlock != null) {
                existingBw.baseBlock.superBlock = bw;
                existingBw.baseBlock = bw;
            } else {
                existingBw.superBlock = bw;
                existingBw.baseBlock = bw;
            }
        } else {
            blockMap.put(name, bw);
        }
    }

    public BlockWriter getBlock(String name) {
        return blockMap.get(name);
    }

}
