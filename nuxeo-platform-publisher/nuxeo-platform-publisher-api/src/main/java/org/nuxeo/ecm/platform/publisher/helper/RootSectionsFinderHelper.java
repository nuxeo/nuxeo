/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RootSectionsFinderHelper {

    private static final Log log = LogFactory.getLog(RootSectionsFinderHelper.class);

    private static Set<String> sectionRootTypes;

    private static Set<String> sectionTypes;

    private RootSectionsFinderHelper() {
        // Helper class
    }

    public static RootSectionsFinder getRootSectionsFinder(CoreSession coreSession) {
        return new RootSectionsFinder(coreSession, getSectionRootTypes(), getSectionTypes());
    }

    private static Set<String> getSectionRootTypes() {
        if (sectionRootTypes == null) {
            sectionRootTypes = getTypeNamesForFacet("MasterPublishSpace");
            if (sectionRootTypes == null) {
                sectionRootTypes = new HashSet<String>();
                sectionRootTypes.add("SectionRoot");
            }
        }
        return sectionRootTypes;
    }

    private static Set<String> getTypeNamesForFacet(String facetName) {
        TypeService schemaService;
        try {
            // XXX should use getService(SchemaManager.class)
            schemaService = (TypeService) Framework.getRuntime().getComponent(
                    TypeService.NAME);
        } catch (Exception e) {
            log.error("Exception in retrieving publish spaces : ", e);
            return null;
        }

        Set<String> publishRoots = schemaService.getTypeManager()
                .getDocumentTypeNamesForFacet(facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    private static Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet("PublishSpace");
            if (sectionTypes == null) {
                sectionTypes = new HashSet<String>();
                sectionTypes.add("Section");
            }
        }
        return sectionTypes;
    }

}
