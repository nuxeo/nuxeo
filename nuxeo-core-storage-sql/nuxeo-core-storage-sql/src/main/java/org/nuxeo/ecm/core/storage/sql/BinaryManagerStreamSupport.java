/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;

import org.nuxeo.runtime.services.streaming.FileSource;

/**
 * Tranfert content to binary storage without 
 * duplicating the source content.
 *
 * @author matic
 * @since 5.6
 */
public interface BinaryManagerStreamSupport {
    Binary getBinary(FileSource source) throws IOException;
}
