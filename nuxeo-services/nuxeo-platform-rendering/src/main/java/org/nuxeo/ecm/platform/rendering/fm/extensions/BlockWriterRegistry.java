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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BlockWriterRegistry {

    protected final Map<String, BlockWriter> blockMap = new HashMap<>();

    public void addBlock(String name, BlockWriter bw) {
        @SuppressWarnings("resource")
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
