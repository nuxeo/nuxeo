/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.resolver;

import java.io.IOException;
import java.io.InputStream;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.nuxeo.runtime.services.streaming.AbstractStreamSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyfacesUploadedFileStreamSource extends AbstractStreamSource {

    private final UploadedFile upFile;

    public MyfacesUploadedFileStreamSource(UploadedFile upFile) {
        this.upFile = upFile;
    }

    public InputStream getStream() throws IOException {
        return upFile.getInputStream();
    }

    @Override
    public boolean canReopen() {
        return true;
    }

    /**
     * @return the upFile.
     */
    public UploadedFile getUploadedFile() {
        return upFile;
    }

    @Override
    public long getLength() throws IOException {
        return upFile.getSize();
    }

}
