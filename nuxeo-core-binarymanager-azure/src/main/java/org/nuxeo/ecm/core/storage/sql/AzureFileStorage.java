/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.core.blob.binary.FileStorage;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public class AzureFileStorage implements FileStorage {
    @Override
    public void storeFile(String key, File file) throws IOException {

    }

    @Override
    public boolean fetchFile(String key, File file) throws IOException {
        return false;
    }

    @Override
    public Long fetchLength(String key) throws IOException {
        return null;
    }
}
