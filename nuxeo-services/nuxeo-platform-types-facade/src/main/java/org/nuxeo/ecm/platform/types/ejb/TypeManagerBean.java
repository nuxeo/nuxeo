/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types.ejb;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

@Stateless
@Local(TypeManagerLocal.class)
@Remote(TypeManager.class)
// @SerializedConcurrentAccess
public class TypeManagerBean implements TypeManagerLocal {

    private static final Log log = LogFactory.getLog(TypeManagerBean.class);

    protected TypeManager typeService;

    @PostConstruct
    public void initialize() {
        typeService = Framework.getLocalService(TypeManager.class);
    }

    public String[] getSuperTypes(String typeName) {
        try {
            return getTypeService().getSuperTypes(typeName);
        } catch (Exception e) {
            log.error("Failed to lookup the SchemaManager service", e);
            return new String[0];
        }
    }

    public Type getType(String typeName) {
        return getTypeService().getType(typeName);
    }

    public boolean hasType(String typeName) {
        return getTypeService().hasType(typeName);
    }

    public Collection<Type> getTypes() {
        return getTypeService().getTypes();
    }

    public Collection<Type> getAllowedSubTypes(String typeName) {
        return getTypeService().getAllowedSubTypes(typeName);
    }

    @Override
    public Collection<Type> getAllowedSubTypes(String typeName,
            DocumentModel currentDoc) {
        return typeService.getAllowedSubTypes(typeName, currentDoc);
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName) {
        return typeService.findAllAllowedSubTypesFrom(typeName);
    }

    @Override
    public Collection<Type> findAllAllowedSubTypesFrom(String typeName,
            DocumentModel currentDoc) {
        return typeService.findAllAllowedSubTypesFrom(typeName, currentDoc);
    }

    @Override
    public Map<String, List<Type>> getTypeMapForDocumentType(String type,
            DocumentModel currentDoc) {
        return typeService.getTypeMapForDocumentType(type, currentDoc);
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName) {
        return typeService.canCreate(typeName, containerTypeName);
    }

    @Override
    public boolean canCreate(String typeName, String containerTypeName,
            DocumentModel currentDoc) {
        return typeService.canCreate(typeName, containerTypeName, currentDoc);
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName) {
        return typeService.isAllowedSubType(typeName, containerTypeName);
    }

    @Override
    public boolean isAllowedSubType(String typeName, String containerTypeName,
            DocumentModel currentDoc) {
        return typeService.isAllowedSubType(typeName, containerTypeName,
                currentDoc);
    }

    @Remove
    public void remove() {
        typeService = null;
    }

    // used to force init when PostConstruct method is not called
    private TypeManager getTypeService() {
        if (typeService == null) {
            initialize();
        }
        return typeService;
    }

}
