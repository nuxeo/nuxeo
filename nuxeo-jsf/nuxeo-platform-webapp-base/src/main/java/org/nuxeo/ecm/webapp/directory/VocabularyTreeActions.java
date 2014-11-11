/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webapp.directory;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Manage localized vocabulary trees. These trees use {@code VocabularyTreeNode}
 *
 * @since 5.5
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@Scope(CONVERSATION)
@Name("vocabularyTreeActions")
@Install(precedence = FRAMEWORK)
public class VocabularyTreeActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VocabularyTreeActions.class);

    public static final String L10NVOCABULARY_SCHEMA = "l10nvocabulary";

    public static final String L10NXVOCABULARY_SCHEMA = "l10nxvocabulary";

    protected transient Map<String, VocabularyTreeNode> treeModels;

    public VocabularyTreeNode get(String vocabularyName,
            boolean displayObsoleteEntries, char keySeparator,
            String orderingField) {
        if (treeModels == null) {
            treeModels = new HashMap<String, VocabularyTreeNode>();
        }
        VocabularyTreeNode treeModel = treeModels.get(vocabularyName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        try {
            Directory directory = Framework.getLocalService(
                    DirectoryService.class).getDirectory(vocabularyName);
            if (directory == null) {
                throw new DirectoryException(vocabularyName
                        + " is not a registered directory");
            }
            String dirSchema = directory.getSchema();
            if (!L10NVOCABULARY_SCHEMA.equals(dirSchema)
                    && !L10NXVOCABULARY_SCHEMA.equals(dirSchema)) {
                throw new DirectoryException(vocabularyName
                        + "does not have the required schema: "
                        + L10NVOCABULARY_SCHEMA + " or "
                        + L10NXVOCABULARY_SCHEMA);
            }
        } catch (DirectoryException e) {
            throw new RuntimeException(e);
        }

        treeModel = new VocabularyTreeNode(0, "", "", "", vocabularyName,
                directoryService, displayObsoleteEntries, keySeparator,
                orderingField);

        treeModels.put(vocabularyName, treeModel);
        return treeModel;
    }

    public List<VocabularyTreeNode> getRoots(String vocabularyName,
            boolean displayObsoleteEntries, char keySeparator,
            String orderingField) {
        return get(vocabularyName, displayObsoleteEntries, keySeparator,
                orderingField).getChildren();
    }

    public String getLabelFor(String vocabularyName, String path,
            char keySeparator) {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        String schemaName = null;
        Session session = null;
        List<String> labels = new ArrayList<String>();
        try {
            DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
            schemaName = directoryService.getDirectorySchema(vocabularyName);
            session = directoryService.open(vocabularyName);
            for (String id : StringUtils.split(path, keySeparator)) {
                String computeLabel = VocabularyTreeNode.computeLabel(locale,
                        session.getEntry(id), schemaName);
                if (computeLabel == null) {
                    labels.add(id);
                } else {
                    labels.add(computeLabel);
                }
            }
        } catch (DirectoryException e) {
            log.error("Error while accessing directory " + vocabularyName, e);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (DirectoryException e) {
                log.error("Error while closing directory " + vocabularyName, e);
            }
        }
        if (labels.isEmpty()) {
            return null;
        } else {
            return StringUtils.join(labels, keySeparator);
        }
    }

    @Observer(EventNames.DIRECTORY_CHANGED)
    public void invalidate(String vocabularyName) {
        if (treeModels != null) {
            treeModels.remove(vocabularyName);
        }
    }

}
