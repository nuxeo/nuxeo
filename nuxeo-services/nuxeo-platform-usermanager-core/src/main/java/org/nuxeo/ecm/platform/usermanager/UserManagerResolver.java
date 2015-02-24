/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * This {@link ObjectResolver} allows to manage integrity for fields containing group or user references.
 * <p>
 * References should have a prefix. NuxeoPrincipal.PREFIX for users, NuxeoGroup.PREFIX for groups.
 * </p>
 * <p>
 * If only user or group are configured, the prefix is not needed but still supported. If noth user and group are
 * configured, reference without prefix are resolved as user first.
 * </p>
 * <p>
 * To use it, put the following code in your schema XSD :
 * </p>
 *
 * <pre>
 * {@code
 * <!-- user or group resolver -->
 * <xs:simpleType name="userOrGroupReference">
 *   <xs:restriction base="xs:string" ref:resolver="userManagerResolver" />
 * </xs:simpleType>
 *
 * <!-- user resolver -->
 * <xs:simpleType name="userReference">
 *   <xs:restriction base="xs:string" ref:resolver="userManagerResolver" ref:type="user" />
 * </xs:simpleType>
 *
 * <!-- group resolver -->
 * <xs:simpleType name="groupReference">
 *   <xs:restriction base="xs:string" ref:resolver="userManagerResolver" ref:type="group" />
 * </xs:simpleType>
 * }
 * </pre>
 *
 * @since 7.1
 */
public class UserManagerResolver implements ObjectResolver {

    public static final String INPUT_PARAM_FILTER = "type";

    public static final String FILTER_GROUP = "group";

    public static final String FILTER_USER = "user";

    public static final String NAME = "userManagerResolver";

    public static final String PARAM_INCLUDE_USERS = "includeUsers";

    public static final String PARAM_INCLUDE_GROUPS = "includeGroups";

    private Map<String, Serializable> parameters;

    private boolean includingUsers = true;

    private boolean includingGroups = true;

    private UserManager userManager;

    public UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getService(UserManager.class);
        }
        return userManager;
    }

    private List<Class<?>> managedClasses = null;

    @Override
    public List<Class<?>> getManagedClasses() {
        if (managedClasses == null) {
            managedClasses = new ArrayList<Class<?>>();
            if (includingUsers) {
                managedClasses.add(NuxeoPrincipal.class);
            }
            if (includingGroups) {
                managedClasses.add(NuxeoGroup.class);
            }
        }
        return managedClasses;
    }

    @Override
    public void configure(Map<String, String> parameters) throws IllegalStateException {
        if (this.parameters != null) {
            throw new IllegalStateException("cannot change configuration, may be already in use somewhere");
        }
        if (FILTER_USER.equals(parameters.get(INPUT_PARAM_FILTER))) {
            includingGroups = false;
        } else if (FILTER_GROUP.equals(parameters.get(INPUT_PARAM_FILTER))) {
            includingUsers = false;
        }
        this.parameters = new HashMap<String, Serializable>();
        this.parameters.put(PARAM_INCLUDE_GROUPS, includingGroups);
        this.parameters.put(PARAM_INCLUDE_USERS, includingUsers);
    }

    @Override
    public String getName() throws IllegalStateException {
        checkConfig();
        return UserManagerResolver.NAME;
    }

    @Override
    public Map<String, Serializable> getParameters() throws IllegalStateException {
        checkConfig();
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean validate(Object value) throws IllegalStateException {
        checkConfig();
        return fetch(value) != null;
    }

    @Override
    public Object fetch(Object value) throws IllegalStateException {
        checkConfig();
        if (value != null && value instanceof String) {
            String name = (String) value;
            boolean userPrefix = name.startsWith(NuxeoPrincipal.PREFIX);
            boolean groupPrefix = name.startsWith(NuxeoGroup.PREFIX);
            if (includingUsers && !includingGroups) {
                if (userPrefix) {
                    name = name.substring(NuxeoPrincipal.PREFIX.length());
                }
                if (LoginComponent.SYSTEM_USERNAME.equals(name)) {
                    return new SystemPrincipal(name);
                }
                return getUserManager().getPrincipal(name);
            } else if (!includingUsers && includingGroups) {
                if (groupPrefix) {
                    name = name.substring(NuxeoGroup.PREFIX.length());
                }
                return getUserManager().getGroup(name);
            } else {
                if (userPrefix) {
                    name = name.substring(NuxeoPrincipal.PREFIX.length());
                    if (LoginComponent.SYSTEM_USERNAME.equals(name)) {
                        return new SystemPrincipal(name);
                    }
                    return getUserManager().getPrincipal(name);
                } else if (groupPrefix) {
                    name = name.substring(NuxeoGroup.PREFIX.length());
                    return getUserManager().getGroup(name);
                } else {
                    if (LoginComponent.SYSTEM_USERNAME.equals(name)) {
                        return new SystemPrincipal(name);
                    }
                    NuxeoPrincipal principal = getUserManager().getPrincipal(name);
                    if (principal != null) {
                        return principal;
                    } else {
                        return getUserManager().getGroup(name);
                    }
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T fetch(Class<T> type, Object value) throws IllegalStateException {
        checkConfig();
        Object principal = fetch(value);
        if (type.isInstance(principal)) {
            return (T) principal;
        }
        return null;
    }

    @Override
    public Serializable getReference(Object entity) throws IllegalStateException {
        checkConfig();
        if (entity != null) {
            if (entity instanceof NuxeoPrincipal && includingUsers) {
                return NuxeoPrincipal.PREFIX + ((NuxeoPrincipal) entity).getName();
            } else if (entity instanceof NuxeoGroup && includingGroups) {
                return NuxeoGroup.PREFIX + ((NuxeoGroup) entity).getName();
            }
        }
        return null;
    }

    @Override
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) throws IllegalStateException {
        checkConfig();
        if (isIncludingUsers() && isIncludingGroups()) {
            return Helper.getConstraintErrorMessage(this, "any", invalidValue, locale);
        } else if (!isIncludingUsers() && isIncludingGroups()) {
            return Helper.getConstraintErrorMessage(this, "group", invalidValue, locale);
        } else if (isIncludingUsers() && !isIncludingGroups()) {
            return Helper.getConstraintErrorMessage(this, "user", invalidValue, locale);
        }
        return String.format("%s cannot resolve reference %s", getName(), invalidValue);
    }

    public boolean isIncludingUsers() throws IllegalStateException {
        checkConfig();
        return includingUsers;
    }

    public boolean isIncludingGroups() throws IllegalStateException {
        checkConfig();
        return includingGroups;
    }

    private void checkConfig() throws IllegalStateException {
        if (parameters == null) {
            throw new IllegalStateException(
                    "you should call #configure(Map<String, String>) before. Please get this resolver throught ExternalReferenceService which is in charge of resolver configuration.");
        }
    }

}
