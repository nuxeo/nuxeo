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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.imageresource;

import java.io.Serializable;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 *
 * An ImageResource represents way to get a Blob Image. Underlying
 * implementation may be Blob based or DocumentModel based.
 *
 * @author tiry
 *
 */
public interface ImageResource extends Serializable {

    Blob getBlob() throws ClientException;

    String getHash() throws ClientException;

    Calendar getModificationDate() throws ClientException;

}
