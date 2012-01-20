/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.diff.service.ComplexItemsDescriptor;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffDisplayService;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implements DocumentDiffDisplayService.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffDisplayServiceImpl extends DefaultComponent implements
        DocumentDiffDisplayService {

    private static final long serialVersionUID = 6608445970773402827L;

    private static final String COMPLEX_ITEMS_DISPLAY_POINT = "complexItemsDisplay";

    private static final String PROPERTY_SCHEMA_FIELD_SEPARATOR = ":";

    /** Complex items contributions. */
    private Map<String, ComplexItemsDescriptor> contributions = new HashMap<String, ComplexItemsDescriptor>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (COMPLEX_ITEMS_DISPLAY_POINT.equals(extensionPoint)) {
            if (contribution instanceof ComplexItemsDescriptor) {
                registerComplexItems((ComplexItemsDescriptor) contribution);
            }
        }
        super.registerContribution(contribution, extensionPoint, contributor);
    }

    /**
     * Registers a complex items contrib.
     * 
     * @param contribution the contribution
     */
    private void registerComplexItems(ComplexItemsDescriptor contribution) {
        contributions.put(contribution.getProperty(), contribution);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ComplexItemsDescriptor> getContributions() {
        return contributions;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getComplexItems(String schemaName, String fieldName) {

        ComplexItemsDescriptor descriptor = contributions.get(schemaName
                + PROPERTY_SCHEMA_FIELD_SEPARATOR + fieldName);
        if (descriptor == null) {
            return null;
        }
        return descriptor.getItems();
    }

    /**
     * {@inheritDoc}
     */
    public void applyComplexItemsOrder(String schemaName, String fieldName,
            List<String> complexItems) {

        List<String> orderedComplexItems = getComplexItems(schemaName,
                fieldName);
        if (orderedComplexItems != null) {
            for (int i = 0; i < orderedComplexItems.size(); i++) {
                String orderedComplexItem = orderedComplexItems.get(i);
                if (complexItems.contains(orderedComplexItem)) {
                    int complexItemIndex = complexItems.indexOf(orderedComplexItem);
                    String tempItem = complexItems.get(i);
                    complexItems.set(i, orderedComplexItem);
                    complexItems.set(complexItemIndex, tempItem);
                }
            }
        }
    }

}
