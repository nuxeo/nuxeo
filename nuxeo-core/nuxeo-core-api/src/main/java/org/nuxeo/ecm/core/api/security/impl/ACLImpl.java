/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.security.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

    public static final ACE BLOCK_INHERITANCE_ACE = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING,
            false);

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
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public boolean add(ACE ace, boolean blockInheritance) {
        boolean aclChanged = false;

        List<ACE> aces = Lists.newArrayList(getACEs());
        String username = ace.getUsername();
        String creator = ace.getCreator();
        if (blockInheritance) {
            if (StringUtils.isEmpty(ace.getCreator())) {
                throw new IllegalArgumentException("Can't block inheritance without a creator");
            }

            aces.clear();
            aces.add(ace);

            if (!username.equals(creator)) {
                aces.add(ACE.builder(creator, SecurityConstants.EVERYTHING).creator(creator).build());
            }

            aces.addAll(getAdminEverythingACES());
            aces.add(BLOCK_INHERITANCE_ACE);
            aclChanged = true;
        } else {
            if (!aces.contains(ace)) {
                int pos = aces.indexOf(BLOCK_INHERITANCE_ACE);
                if (pos >= 0) {
                    aces.add(pos, ace);
                } else {
                    aces.add(ace);
                }
                aclChanged = true;
            }
        }

        setACEs(aces.toArray(new ACE[aces.size()]));

        return aclChanged;
    }

    protected List<ACE> getAdminEverythingACES() {
        List<ACE> aces = new ArrayList<>();
        AdministratorGroupsProvider provider = Framework.getLocalService(AdministratorGroupsProvider.class);
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
