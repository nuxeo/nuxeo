/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.ecm.directory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * Wraps a session, enforcing permission access
 *
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 5.9.6
 */
public class SecuredSession implements Session {

    protected static final String POWER_USERS_GROUP = "powerusers";

    public static Session wrap(Directory directory,
            PermissionDescriptor[] permissions, Session session) {
        return new SecuredSession(directory, permissions, session);
    }

    public static Session unwrap(Session session) {
        if (session instanceof SecuredSession) {
            return ((SecuredSession)session).delegate;
        }
        return session;
    }

    protected final Directory directory;

    protected final NuxeoPrincipal owner = ClientLoginModule
        .getCurrentPrincipal();

    protected final PermissionDescriptor[] permissions;

    protected final Session delegate;

    protected SecuredSession(Directory directory, PermissionDescriptor[] perms,
            Session delegate) {
        super();
        this.delegate = delegate;
        this.directory = directory;
        permissions = perms;
    }

    @Override
    public boolean isAllowed(Right right) {

        if (owner == null) {
            return true;
        }

        String username = owner.getName();
        if (username.equalsIgnoreCase(LoginComponent.SYSTEM_USERNAME)) {
            return true;
        }
        if (owner.isAdministrator()) {
            return true;
        }
        if (owner.isMemberOf(POWER_USERS_GROUP)) {
            return true;
        }

        if (permissions.length == 0) {

            // Return true for read access to anyone when nothing defined
            if (right.equals(Right.Read)) {
                return true;
            }

            return false;
        }

        for (PermissionDescriptor each : permissions) {
            if (each.isAllowed(owner, right)) {
                return true;
            }
        }

        for (PermissionDescriptor each : permissions) {
            if (each.isAllowed(owner, right)) {
                return true;
            }
        }

        // If the permission has not been found and if the permission to
        // check is read
        // Then try to check if the current user is allowed, because having
        // write access include read
        if (Right.Read.equals(right)) {
            return isAllowed(Right.Write);
        }

        return false;

    }

    protected void checkAllowed(Right right) {
        if (!isAllowed(right)) {
            throw new DirectorySecurityException(this, right);
        }
    }

    @Override
    public DocumentModel getEntry(String id) throws DirectoryException {
        checkAllowed(Right.Read);
        return delegate.getEntry(id);
    }

    @Override
    public DocumentModel getEntry(String id, boolean fetchReferences)
            throws DirectoryException {
        checkAllowed(Right.Read);
        return delegate.getEntry(id, fetchReferences);
    }

    @Override
    public DocumentModelList getEntries() throws ClientException,
            DirectoryException {
        checkAllowed(Right.Read);
        return delegate.getEntries();
    }

    @Override
    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws ClientException, DirectoryException {
        checkAllowed(Right.Write);
        return delegate.createEntry(fieldMap);
    }

    @Override
    public void updateEntry(DocumentModel docModel) throws ClientException,
            DirectoryException {
        checkAllowed(Right.Write);
        delegate.updateEntry(docModel);
    }

    @Override
    public void deleteEntry(DocumentModel docModel) throws ClientException,
            DirectoryException {
        checkAllowed(Right.Write);
        delegate.deleteEntry(docModel);
    }

    @Override
    public void deleteEntry(String id) throws ClientException,
            DirectoryException {
        checkAllowed(Right.Write);
        delegate.deleteEntry(id);
    }

    @Override
    public void deleteEntry(String id, Map<String, String> map)
            throws ClientException, DirectoryException {
        checkAllowed(Right.Write);
        delegate.deleteEntry(id, map);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter)
            throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.query(filter);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.query(filter, fulltext);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.query(filter, fulltext, orderBy);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.query(filter, fulltext, orderBy, fetchReferences);
    }

    @Override
    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences, int limit, int offset)
            throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.query(filter, fulltext, orderBy, fetchReferences,
                limit, offset);
    }

    @Override
    public void close() throws DirectoryException {
        delegate.close();
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            String columnName) throws ClientException, DirectoryException {
        checkAllowed(Right.Read);
        return delegate.getProjection(filter, columnName);
    }

    @Override
    public List<String> getProjection(Map<String, Serializable> filter,
            Set<String> fulltext, String columnName) throws ClientException,
            DirectoryException {
        checkAllowed(Right.Read);
        return delegate.getProjection(filter, fulltext, columnName);
    }

    @Override
    public boolean isAuthenticating() throws ClientException,
            DirectoryException {
        return delegate.isAuthenticating();
    }

    @Override
    public boolean authenticate(String username, String password)
            throws ClientException, DirectoryException {
        return delegate.authenticate(username, password);
    }

    @Override
    public String getIdField() throws ClientException {
        return delegate.getIdField();
    }

    @Override
    public String getPasswordField() throws ClientException {
        return delegate.getPasswordField();
    }

    @Override
    public boolean isReadOnly() throws ClientException {
        return delegate.isReadOnly();
    }

    @Override
    public boolean hasEntry(String id) throws ClientException {
        checkAllowed(Right.Read);
        return delegate.hasEntry(id);
    }

    @Override
    public DocumentModel createEntry(DocumentModel entry)
            throws ClientException {
        checkAllowed(Right.Write);
        return delegate.createEntry(entry);
    }

}
