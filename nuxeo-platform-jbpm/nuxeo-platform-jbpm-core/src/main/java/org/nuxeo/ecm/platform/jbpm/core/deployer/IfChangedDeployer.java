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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core.deployer;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.nuxeo.ecm.platform.jbpm.AbstractProcessDefinitionDeployer;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated see https://jira.nuxeo.org/browse/NXP-4650 , use nuxeoProperties deployer instead.
 */
@Deprecated
public class IfChangedDeployer extends AbstractProcessDefinitionDeployer implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient MD5Hasher hasher;

    private transient HashPersistence persistence;

    private final Map<URL, String> hashes = new HashMap<URL, String>();

    public IfChangedDeployer() {
        hasher = new MD5Hasher();
        persistence = new HashPersistence();
    }

    @Override
    public boolean isDeployable(URL url) throws NoSuchAlgorithmException,
            SAXException, IOException, TransformerException, ParserConfigurationException {
        String hash = getHasher().getMD5FromURL(url);
        hashes.put(url, hash);
        return getPersistence().exists(hash);
    }

    @Override
    public void deploy(URL url) throws Exception {
        super.deploy(url);
        getPersistence().persist(hashes.get(url));
    }

    public MD5Hasher getHasher() {
        if (hasher==null) {
            hasher = new MD5Hasher();
        }
        return hasher;
    }

    public HashPersistence getPersistence() {
        if (persistence==null) {
            persistence = new HashPersistence();
        }
        return persistence;
    }

}
