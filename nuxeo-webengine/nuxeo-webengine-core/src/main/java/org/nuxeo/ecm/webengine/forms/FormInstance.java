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

package org.nuxeo.ecm.webengine.forms;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface FormInstance {

    Collection<String> getKeys() throws WebException;

    Object[] get(String key) throws WebException;

    String getString(String key) throws WebException;

    String[] getList(String key) throws WebException;

    Blob getBlob(String key) throws WebException;

    Blob[] getBlobs(String key) throws WebException;

    Map<String, String[]> getFormFields() throws WebException;

    Map<String, Blob[]> getBlobFields() throws WebException;

    void fillDocument(DocumentModel doc) throws WebException;
}


