/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.cache;

import java.io.File;
import java.util.ArrayList;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Cachable implementation of the {@link SimpleBlobHolder}
 *
 * @author tiry
 *
 */
public class SimpleCachableBlobHolder extends SimpleBlobHolder implements CachableBlobHolder{

    public SimpleCachableBlobHolder(Blob blob) {
        super(blob);
    }

    public SimpleCachableBlobHolder(String path) {
        super(new FileBlob(new File(path)));
    }

    public void load(String path) {
         blobs = new ArrayList<Blob>();
         blobs.add(new FileBlob(new File(path)));
    }

    public String persist(String basePath) throws Exception{
        Path path = new Path(basePath);
        path = path.append(getHash());
        File file = new File(path.toString());
        getBlob().transferTo(file);
        return file.getAbsolutePath();
    }


}
