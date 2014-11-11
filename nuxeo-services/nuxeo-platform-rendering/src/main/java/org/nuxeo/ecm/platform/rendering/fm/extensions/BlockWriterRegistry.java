/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
