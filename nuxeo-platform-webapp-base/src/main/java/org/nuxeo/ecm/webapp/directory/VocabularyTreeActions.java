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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Manage localized vocabulary trees. These trees use {@code VocabularyTreeNode}
 *
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

    protected DirectoryService directoryService;

    public VocabularyTreeNode get(String vocabularyName) {
        return get(vocabularyName, false, false);
    }

    public VocabularyTreeNode get(String vocabularyName, boolean showObsolete, boolean sortByOrdering) {
        if (treeModels == null) {
            treeModels = new HashMap<String, VocabularyTreeNode>();
        }
        VocabularyTreeNode treeModel = treeModels.get(vocabularyName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        try {
            Directory directory = getDirectoryService().getDirectory(
                    vocabularyName);
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
                directoryService, showObsolete, sortByOrdering);

        treeModels.put(vocabularyName, treeModel);
        return treeModel;
    }

    public List<VocabularyTreeNode> getRoots(String vocabularyName, boolean showObsolete, boolean sortByOrdering) {
        return get(vocabularyName, showObsolete, sortByOrdering).getChildren();
    }

    public String getLabelFor(String vocabularyName, String pathOrId) {
        return getLabelFor(vocabularyName, pathOrId, false);
    }

    public String getLabelFor(String vocabularyName, String pathOrId,
            boolean idOnly) {
        if (idOnly) {
            return getLabelFromId(vocabularyName, pathOrId);
        }
        return getLabelFromPath(vocabularyName, pathOrId);
    }

    protected String getLabelFromPath(String vocabularyName, String fullPath) {
        VocabularyTreeNode rootNode = get(vocabularyName);
        List<String> labels = new ArrayList<String>();
        computeLabels(labels, rootNode, fullPath);
        return StringUtils.join(labels, "/");
    }

    protected void computeLabels(List<String> labels, VocabularyTreeNode node,
            String fullPath) {
        if (!node.getPath().isEmpty()) {
            labels.add(node.getLabel());
        }
        if (fullPath.equals(node.getPath())) {
            return;
        }
        for (VocabularyTreeNode treeNode : node.getChildren()) {
            if (fullPath.startsWith(treeNode.getPath())) {
                computeLabels(labels, treeNode, fullPath);
            }
        }
    }

    protected String getLabelFromId(String vocabularyName, String id) {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        String schemaName = null;
        Session session = null;
        DocumentModel entry = null;
        try {
            schemaName = getDirectoryService().getDirectorySchema(
                    vocabularyName);
            session = getDirectoryService().open(vocabularyName);
            entry = session.getEntry(id);
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
        if (entry != null && schemaName != null) {
            return VocabularyTreeNode.computeLabel(locale, entry, schemaName);
        } else {
            return null;
        }
    }

    protected DirectoryService getDirectoryService() {
        if (directoryService == null) {
            directoryService = DirectoryHelper.getDirectoryService();
        }
        return directoryService;
    }

    @Observer(EventNames.DIRECTORY_CHANGED)
    public void invalidate(String vocabularyName) {
        if (treeModels != null) {
            treeModels.remove(vocabularyName);
        }
    }

}
