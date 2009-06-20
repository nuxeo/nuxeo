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
 */
package org.nuxeo.build.ant.profile;

import org.apache.tools.ant.BuildException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProfileGroup {

    protected Profile defaultProfile;
    protected Profile[] profiles;

    public ProfileGroup(AntProfileManager mgr, String[] profiles, String defaultProfile) {
        if (defaultProfile != null) {
            this.defaultProfile = mgr.getOrCreateProfile(defaultProfile);
        }
        this.profiles = new Profile[profiles.length];
        Profile activeProfile = null;
        for (int i=0; i<profiles.length; i++) {
            String profileName = profiles[i];
            Profile profile = mgr.getOrCreateProfile(profileName);
            if (profile.group != null) {
                throw new BuildException("A profile is part of 2 distinct groups: "+profileName);
            }
            profile.group = this;
            if (profile.isActive()) {
                if (activeProfile != null) {
                    throw new BuildException("Profile Group has 2 active profiles: "+activeProfile.getName()+", "+profileName);
                }
                activeProfile = profile;
            }
            this.profiles[i] = profile;
        }
        if (activeProfile == null && this.defaultProfile != null) {
            this.defaultProfile._setActive(true);
        }
    }

    void activateProfile(Profile profile, boolean isActive) {
        profile._setActive(isActive);
        for (Profile p : profiles) {
            if (p != profile) {
                p._setActive(false);
            }
        }
        if (!isActive) {
            defaultProfile._setActive(true);
        }
    }

    public Profile getActiveProfile() {
        for (Profile p : profiles) {
            if (p.isActive()) {
                return p;
            }
        }
        return null;
    }

}
