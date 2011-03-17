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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractStreamSource implements StreamSource {

    @Override
    public long getLength() throws IOException {
        return -1L;
    }

    @Override
    public boolean canReopen() {
        return false;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return FileUtils.readBytes(getStream());
    }

    @Override
    public String getString() throws IOException {
        return new String(getBytes());
    }

    @Override
    public void copyTo(File file) throws IOException {
        copyTo(new FileOutputStream(file));
    }

    @Override
    public void copyTo(OutputStream out) throws IOException {
        FileUtils.copy(getStream(), out);
    }

    @Override
    public void destroy() {
        // do nothing
    }

}
