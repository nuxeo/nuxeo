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

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Profile;
import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProfileRegistry extends AbstractContributionRegistry<String, ProfileDescriptor> {

    protected Map<String, Profile> profiles = new ConcurrentHashMap<String, Profile>();

    protected WebEngine engine;


    public ProfileRegistry(WebEngine engine) {
        this.engine = engine;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public void putProfile(Profile app) {
        profiles.put(app.getName(), app);
    }

    public Profile removeProfile(String id) {
        return profiles.remove(id);
    }

    public Profile getProfile(String id) {
        return profiles.get(id);
    }

    public Profile[] getProfiles() {
        return profiles.values().toArray(new Profile[profiles.size()]);
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        profiles.clear();
    }

    protected ProfileDescriptor clone(ProfileDescriptor descriptor) {
        return descriptor.clone();
    }
    
    @Override
    protected void applyFragment(ProfileDescriptor object, ProfileDescriptor fragment) {
        if (fragment.guardDescriptor != null) {
            object.guardDescriptor = fragment.guardDescriptor;
        }
        if (fragment.roots != null) {
            if (object.roots == null) {
                object.roots = new ArrayList<String>();
            }
            object.roots.addAll(fragment.roots);
        }
    }

    @Override
    protected void installContribution(String key, ProfileDescriptor object) {
        try {
            Profile app = new ProfileImpl(engine, object.directory, object);
            profiles.put(key, app);
        } catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }

    @Override
    protected void updateContribution(String key, ProfileDescriptor object) {
        installContribution(key, object);
    }

    @Override
    protected void uninstallContribution(String key) {
        profiles.remove(key);
    }
    
    @Override
    protected boolean isMainFragment(ProfileDescriptor object) {
        return object.fragment == null || object.fragment.length() == 0;
    }

    public void registerDescriptor(File root, ProfileDescriptor desc) {
        desc.directory = root;
        addFragment(desc.name, desc, desc.base);
    }

    public void unregisterDescriptor(ProfileDescriptor desc) {
        removeFragment(desc.name, desc);
    }

}
