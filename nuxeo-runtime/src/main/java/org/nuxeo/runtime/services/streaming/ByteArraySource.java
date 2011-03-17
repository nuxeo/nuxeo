/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.services.streaming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ByteArraySource extends AbstractStreamSource {

    protected final byte[] bytes;


    public ByteArraySource(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public long getLength() throws IOException {
        return bytes.length;
    }

    @Override
    public boolean canReopen() {
        return true;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public byte[] getBytes() throws IOException {
        return bytes;
    }

}
