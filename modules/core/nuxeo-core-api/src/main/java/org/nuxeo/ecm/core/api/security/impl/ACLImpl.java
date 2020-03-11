/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.Framework;

import com.google.common.collect.Lists;

/**
 * An ACL implementation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ACLImpl extends ArrayList<ACE>implements ACL {

    private static final long serialVersionUID = 5332101749929771434L;

    private static final Log log = LogFactory.getLog(ACLImpl.class);

    private final String name;

    private final boolean isReadOnly;

    public ACLImpl(String name, boolean isReadOnly) {
        this.name = name;
        this.isReadOnly = isReadOnly;
    }

    public ACLImpl() {
        this(LOCAL_ACL, false);
    }

    public ACLImpl(String name) {
        this(name, false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ACE[] getACEs() {
        return toArray(new ACE[size()]);
    }

    @Override
    public void setACEs(ACE[] aces) {
        clear();
        addAll(Arrays.asList(aces));
        warnForDuplicateACEs(aces);
    }

    private void warnForDuplicateACEs(ACE[] aces) {
        if (! log.isWarnEnabled() || ACL.INHERITED_ACL.equals(name)) {
            return;
        }
        Set<ACE> aceSet = new HashSet<>(aces.length);
        for (ACE ace : aces) {
            if (!aceSet.add(ace)) {
                Throwable throwable = null;
                if (log.isTraceEnabled()) {
                    throwable = new Throwable();
                }
                log.warn("Setting an ACL with at least one duplicate entry: " + ace + ", ACL entries: " + Arrays.toString(aces),
                        throwable);
                break;
            }
        }
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public boolean blockInheritance(String username) {
        boolean aclChanged = false;
        List<ACE> aces = Lists.newArrayList(getACEs());
        if (!aces.contains(ACE.BLOCK)) {
            aces.add(ACE.builder(username, SecurityConstants.EVERYTHING).creator(username).build());
            aces.addAll(getAdminEverythingACES());
            aces.add(ACE.BLOCK);
            aclChanged = true;
            setACEs(aces.toArray(new ACE[aces.size()]));
        }
        return aclChanged;
    }

    @Override
    public boolean unblockInheritance() {
        boolean aclChanged = false;
        List<ACE> aces = Lists.newArrayList(getACEs());
        if (aces.contains(ACE.BLOCK)) {
            aces.remove(ACE.BLOCK);
            aclChanged = true;
            setACEs(aces.toArray(new ACE[aces.size()]));
        }
        return aclChanged;
    }

    @Override
    public boolean add(ACE ace) {
        boolean aclChanged = false;
        List<ACE> aces = Lists.newArrayList(getACEs());
        if (!aces.contains(ace)) {
            int pos = aces.indexOf(ACE.BLOCK);
            if (pos >= 0) {
                aces.add(pos, ace);
            } else {
                aces.add(ace);
            }
            aclChanged = true;
            setACEs(aces.toArray(new ACE[aces.size()]));
        }

        return aclChanged;
    }

    protected List<ACE> getAdminEverythingACES() {
        List<ACE> aces = new ArrayList<>();
        AdministratorGroupsProvider provider = Framework.getService(AdministratorGroupsProvider.class);
        List<String> administratorsGroups = provider.getAdministratorsGroups();
        for (String adminGroup : administratorsGroups) {
            aces.add(new ACE(adminGroup, SecurityConstants.EVERYTHING, true));
        }
        return aces;
    }

    @Override
    public boolean replace(ACE oldACE, ACE newACE) {
        boolean aclChanged = false;
        int index = indexOf(oldACE);
        if (index != -1) {
            remove(oldACE);
            add(index, newACE);
            aclChanged = true;
        }

        return aclChanged;
    }

    @Override
    public boolean removeByUsername(String username) {
        boolean aclChanged = false;

        List<ACE> aces = Lists.newArrayList(getACEs());
        for (Iterator<ACE> it = aces.iterator(); it.hasNext();) {
            ACE ace = it.next();
            if (ace.getUsername().equals(username)) {
                it.remove();
                aclChanged = true;
            }
        }
        setACEs(aces.toArray(new ACE[aces.size()]));

        return aclChanged;
    }

    @Override
    public Object clone() {
        ACLImpl copy = new ACLImpl(name, isReadOnly);
        ACE[] aces = new ACE[size()];
        for (int i = 0; i < size(); i++) {
            aces[i] = (ACE) get(i).clone();
        }
        copy.setACEs(aces);
        return copy;
    }

}
