/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    /**
     * @deprecated since 5.9.2.
     */
    @Deprecated
    public static final String L10NVOCABULARY_SCHEMA = "l10nvocabulary";

    /**
     * @deprecated since 5.9.2.
     */
    @Deprecated
    public static final String L10NXVOCABULARY_SCHEMA = "l10nxvocabulary";

    protected transient Map<String, VocabularyTreeNode> treeModels;

    public VocabularyTreeNode get(String vocabularyName, boolean displayObsoleteEntries, char keySeparator,
            String orderingField) {
        if (treeModels == null) {
            treeModels = new HashMap<>();
        }
        VocabularyTreeNode treeModel = treeModels.get(vocabularyName);
        if (treeModel != null) {
            // return cached model
            return treeModel;
        }
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try {
            Directory directory = Framework.getService(DirectoryService.class).getDirectory(vocabularyName);
            if (directory == null) {
                throw new DirectoryException(vocabularyName + " is not a registered directory");
            }
        } catch (DirectoryException e) {
            throw new RuntimeException(e);
        }

        treeModel = new VocabularyTreeNode(0, "", "", "", vocabularyName, directoryService, displayObsoleteEntries,
                keySeparator, orderingField);

        treeModels.put(vocabularyName, treeModel);
        return treeModel;
    }

    public List<VocabularyTreeNode> getRoots(String vocabularyName, boolean displayObsoleteEntries, char keySeparator,
            String orderingField) {
        return get(vocabularyName, displayObsoleteEntries, keySeparator, orderingField).getChildren();
    }

    public String getLabelFor(String vocabularyName, String path, char keySeparator) {
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        String schemaName = null;
        List<String> labels = new ArrayList<>();
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        try (Session session = directoryService.open(vocabularyName)) {
            schemaName = directoryService.getDirectorySchema(vocabularyName);
            for (String id : StringUtils.split(path, keySeparator)) {
                DocumentModel entry = session.getEntry(id);
                if (entry != null) {
                    String computeLabel = VocabularyTreeNode.computeLabel(locale, entry, schemaName);
                    if (computeLabel == null) {
                        labels.add(id);
                    } else {
                        labels.add(computeLabel);
                    }
                } else {
                    labels.add(id);
                }
            }
        } catch (DirectoryException e) {
            log.error("Error while accessing directory " + vocabularyName, e);
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
