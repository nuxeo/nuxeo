package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.schema.types.reference.ExternalReferenceResolver;
import org.nuxeo.runtime.api.Framework;

public class UserManagerResolver implements ExternalReferenceResolver {

    public static final String INPUT_PARAM_FILTER = "filter";

    public static final String FILTER_GROUP = "group";

    public static final String FILTER_USER = "user";

    public static final String NAME = "UserManagerReference";

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

    @Override
    public void configure(Map<String, String> parameters) throws IllegalArgumentException {
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
    public String getName() {
        checkConfig();
        return UserManagerResolver.NAME;
    }

    @Override
    public Map<String, Serializable> getParameters() {
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
            if (includingUsers) {
                String name = (String) value;
                if (name.startsWith(NuxeoPrincipal.PREFIX)) {
                    String username = name.substring(NuxeoPrincipal.PREFIX.length());
                    return getUserManager().getPrincipal(username);
                }
            }
            if (includingGroups) {
                String name = (String) value;
                if (name.startsWith(NuxeoGroup.PREFIX)) {
                    String groupname = name.substring(NuxeoGroup.PREFIX.length());
                    return getUserManager().getGroup(groupname);
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
    public Serializable getReference(Object entity) throws IllegalStateException, IllegalArgumentException {
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
    public String getConstraintErrorMessage(Object invalidValue, Locale locale) {
        checkConfig();
        String computedInvalidValue = null;
        if (invalidValue != null) {
            String invalidValueString = invalidValue.toString();
            if (invalidValueString.length() > 20) {
                computedInvalidValue = invalidValueString.substring(0, 15) + "...";
            } else {
                computedInvalidValue = invalidValueString;
            }
        }
        if (includingGroups && includingUsers) {
            return String.format("%s must be a user name or a group name", computedInvalidValue);
        } else if (includingGroups) {
            return String.format("%s must be a group name", computedInvalidValue);
        } else if (includingUsers) {
            return String.format("%s must be a user name", computedInvalidValue);
        }
        return "invalid value";
    }

    public boolean isIncludingUsers() {
        checkConfig();
        return includingUsers;
    }

    public boolean isIncludingGroups() {
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
