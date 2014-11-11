/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: AbstractPlugin.java 4105 2006-10-15 12:29:25Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager.service.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_PROPERTIES;

/**
 * File importer abstract class.
 * <p>
 * Default file importer behavior.
 *
 * @see FileImporter
 *
 * @author <a href="mailto:akalogeropoulos@nuxeo.com">Andreas Kalogeropolos</a>
 */
public abstract class AbstractFileImporter implements FileImporter {

    private static final long serialVersionUID = 1L;

    protected String name = "";

    protected List<String> filters = new ArrayList<String>();

    protected List<Pattern> patterns;

    protected boolean enabled = true;

    protected Integer order = 0;

    // to be used by plugin implementation to gain access to standard file
    // creation utility methods without having to lookup the service
    protected FileManagerService fileManagerService;

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
        patterns = new ArrayList<Pattern>();
        for (String filter : filters) {
            patterns.add(Pattern.compile(filter));
        }
    }

    public boolean matches(String mimeType) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(mimeType).matches()) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DocumentModel create(CoreSession documentManager, File file,
            String path, boolean overwrite, String mimeType,
            TypeManager typService) {
        return null;
    }

    public FileManagerService getFileManagerService() {
        return fileManagerService;
    }

    public void setFileManagerService(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public int compareTo(FileImporter other) {
        Integer otherOrder = other.getOrder();
        if (order == null && otherOrder == null) {
            return 0;
        } else if (order == null) {
            return 1;
        } else if (otherOrder == null) {
            return -1;
        }
        return order.compareTo(otherOrder);
    }

    // TODO: remove
    protected TypeManager getTypeService() throws ClientException {
        try {
            return Framework.getService(TypeManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    /**
     * Returns nearest container path
     * <p>
     * If given path points to a folderish document, return it. Else, return
     * parent path.
     */
    protected String getNearestContainerPath(CoreSession documentManager,
            String path) throws ClientException {
        DocumentModel currentDocument = documentManager.getDocument(new PathRef(
                path));
        if (!currentDocument.isFolder()) {
            path = path.substring(0, path.lastIndexOf('/'));
        }
        return path;
    }

    protected DocumentModel overwriteAndIncrementversion(
            CoreSession documentManager, DocumentModel doc)
            throws ClientException {
        doc.putContextData(VersioningService.VERSIONING_OPTION,
                VersioningOption.MINOR);
        return documentManager.saveDocument(doc);
    }

    protected void doSecurityCheck(CoreSession documentManager, String path,
            String typeName, TypeManager typeService)
            throws DocumentSecurityException, ClientException {
        // perform the security checks
        PathRef containerRef = new PathRef(path);
        if (!documentManager.hasPermission(containerRef, READ_PROPERTIES)
                || !documentManager.hasPermission(containerRef, ADD_CHILDREN)) {
            throw new DocumentSecurityException(
                    "Not enough rights to create folder");
        }
        DocumentModel container = documentManager.getDocument(containerRef);

        Type containerType = typeService.getType(container.getType());
        if (containerType == null) {
            return;
        }

        if (!typeService.isAllowedSubType(typeName, container.getType(),
                container)) {
            throw new ClientException(
                    String.format(
                            "Cannot create document of type %s in container with type %s",
                            typeName, containerType.getId()));
        }
    }

}
