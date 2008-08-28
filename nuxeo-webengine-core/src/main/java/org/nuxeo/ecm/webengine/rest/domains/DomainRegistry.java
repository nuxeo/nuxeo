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

package org.nuxeo.ecm.webengine.rest.domains;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.RootDescriptor;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DomainRegistry extends AbstractContributionRegistry<String, DomainDescriptor> {

    protected Map<String, WebDomain> domains = new ConcurrentHashMap<String, WebDomain>();

    protected WebEngine2 engine;


    public DomainRegistry(WebEngine2 engine) {
        this.engine = engine;
    }

    public WebEngine2 getEngine() {
        return engine;
    }

    public void putDomain(WebDomain domain) {
        domains.put(domain.getId(), domain);
    }

    public WebDomain removeDomain(String id) {
        return domains.remove(id);
    }

    public WebDomain getDomain(String id) {
        return domains.get(id);
    }

    public WebDomain[] getDomains() {
        return domains.values().toArray(new WebDomain[domains.size()]);
    }

    @Override
    public synchronized void clear() {
        super.clear();
        domains.clear();
    }

    @Override
    protected void applyFragment(DomainDescriptor object, DomainDescriptor fragment) {
        if (fragment.contentRoot != null) {
            object.contentRoot = fragment.contentRoot;
        }
        if (fragment.defaultPage != "default.ftl") {
            object.defaultPage = fragment.defaultPage;
        }
        if (fragment.errorPage != "error.ftl") {
            object.errorPage = fragment.errorPage;
        }
        if (fragment.indexPage != "index.ftl") {
            object.indexPage = fragment.indexPage;
        }
        if (fragment.guardDescriptor != null) {
            object.guardDescriptor = fragment.guardDescriptor;
        }
        if (fragment.type != null) {
            object.type = fragment.type;
        }
        if (fragment.roots != null) {
            if (object.roots == null) {
                object.roots = new ArrayList<RootDescriptor>();
            }
            object.roots.addAll(fragment.roots);
        }
    }

    @Override
    protected void installContribution(String key, DomainDescriptor object) {
        WebDomain domain = null;
        try {
            if (object.contentRoot != null) {
                domain = new DocumentDomain(engine, object);
            } else if (object.type != null) {
                domain = new DefaultWebDomain<DomainDescriptor>(engine, object);
            } else {
                domain = new ScriptDomain(engine, object);
            }
            domains.put(key, domain);
        } catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    protected void reinstallContribution(String key, DomainDescriptor object) {
        installContribution(key, object);
    }

    @Override
    protected void uninstallContribution(String key) {
        domains.remove(key);
    }

    public void registerDescriptor(DomainDescriptor desc) {
        addFragment(desc.id, desc, desc.base);
    }

    public void unregisterDescriptor(DomainDescriptor desc) {
        removeFragment(desc.id, desc);
    }

}
