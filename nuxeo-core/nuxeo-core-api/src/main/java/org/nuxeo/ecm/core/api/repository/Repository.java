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

package org.nuxeo.ecm.core.api.repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceGroup;
import org.nuxeo.runtime.api.ServiceManager;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("repository")
public class Repository implements Serializable {

    private static final long serialVersionUID = -5884097487266847648L;

    @XNode("@repositoryUri")
    private String repositoryUri;

    @XNode("@name")
    private String name;

    @XNode("@group")
    private String group;

    @XNode("@label")
    private String label;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> properties;

    @XNode("@supportsTags")
    protected Boolean supportsTags=null;

    public Repository() {
    }

    public Repository(String name, String label) {
        this.name = name;
        this.label = label;
        properties = new HashMap<String, String>();
    }

    public Repository(String name) {
        this(name, name);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public String getProperty(String name, String defValue) {
        String val = properties.get(name);
        if (val == null) {
            return defValue;
        }
        return val;
    }

    public String removeProperty(String name) {
        return properties.remove(name);
    }

    public String setProperty(String name, String value) {
        return properties.put(name, value);
    }


    public CoreSession open() throws Exception {
        return open(new HashMap<String, Serializable>());
    }

    protected CoreSession lookupSession() throws Exception {
        CoreSession session;
        if (group != null) {
            ServiceManager mgr = Framework.getLocalService(ServiceManager.class);
            ServiceGroup sg = mgr.getGroup(group);
            if (sg == null) {
                // TODO maybe throw other exception
                throw new ClientException("group '" + group + "' not defined");
            }
            session = sg.getService(CoreSession.class, name);
        } else {
            session = Framework.getService(CoreSession.class, name);
        }
        return session;
    }

    public boolean supportsTags() throws Exception {
        if (supportsTags==null) {
            CoreSession unconnectedSession =lookupSession();
            supportsTags =  unconnectedSession.supportsTags(name);
            // avoid leaking DocumentManagerBean
            unconnectedSession.destroy();
        }
        return supportsTags;
    }

    public CoreSession open(Map<String, Serializable> context) throws Exception {
        CoreSession session = lookupSession();
        if (repositoryUri == null) {
            repositoryUri = name;
        }
        String sid = session.connect(repositoryUri, context);
        // register session on local JVM so it can be used later by doc models
        CoreInstance.getInstance().registerSession(sid, session);
        return session;
    }

    public static void close(CoreSession session) {
        CoreInstance.getInstance().close(session);
    }

    public static RepositoryInstance newRepositoryInstance(Repository repository) {
        return new RepositoryInstanceHandler(repository).getProxy();
    }

    public RepositoryInstance newInstance() {
        return newRepositoryInstance(this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(Repository.class.getSimpleName());
        buf.append(" {name=").append(name);
        buf.append(", label=").append(label);
        buf.append('}');

        return buf.toString();
    }

    public String getRepositoryUri() {
        return repositoryUri;
    }

}
