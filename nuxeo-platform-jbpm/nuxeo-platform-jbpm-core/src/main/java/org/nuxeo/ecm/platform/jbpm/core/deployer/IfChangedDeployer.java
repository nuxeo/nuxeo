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
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.nuxeo.ecm.platform.jbpm.AbstractProcessDefinitionDeployer;
import org.nuxeo.ecm.platform.jbpm.ProcessDefinitionDeployer;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class IfChangedDeployer extends AbstractProcessDefinitionDeployer
        implements ProcessDefinitionDeployer {

    private final MD5Hasher hasher;

    private final HashPersistence persistence;

    private final Map<URL, String> hashes = new HashMap<URL, String>();

    public IfChangedDeployer() throws TransformerConfigurationException,
            ParserConfigurationException {
        hasher = new MD5Hasher();
        persistence = new HashPersistence();
    }

    @Override
    public boolean isDeployable(URL url) throws NoSuchAlgorithmException,
            SAXException, IOException, TransformerException {
        String hash = hasher.getMD5FromURL(url);
        hashes.put(url, hash);
        return persistence.exists(hash);
    }

    @Override
    public void deploy(URL url) throws Exception {
        super.deploy(url);
        persistence.persist(hashes.get(url));
    }
}
