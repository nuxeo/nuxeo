/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.filter;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.base.CharMatcher;

/**
 * @since 9.1
 */
public class CharacterFilteringServiceImpl extends DefaultComponent implements CharacterFilteringService {

    public static final String FILTERING_XP = "filtering";

    protected CharacterFilteringServiceDescriptor desc;

    protected CharMatcher charsToRemove;

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (FILTERING_XP.equals(point)) {

            desc = (CharacterFilteringServiceDescriptor) contrib;

            CharMatcher charsToPreserve = CharMatcher.anyOf("\r\n\t");
            CharMatcher allButPreserved = charsToPreserve.negate();
            charsToRemove = CharMatcher.javaIsoControl().and(allButPreserved);
            charsToRemove = charsToRemove.or(CharMatcher.invisible().and(CharMatcher.whitespace().negate()));

            List<String> additionalChars = desc.getDisallowedChars();
            if (additionalChars != null && !additionalChars.isEmpty()) {
                String otherCharsToRemove = additionalChars.stream().map(StringEscapeUtils::unescapeJava).collect(
                        Collectors.joining());
                charsToRemove = charsToRemove.or(CharMatcher.anyOf(otherCharsToRemove));
            }
        } else {
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public String filter(String value) {
        return charsToRemove.removeFrom(value);
    }

    @Override
    public void filter(DocumentModel docModel) {
        if (desc.isEnabled()) {
            // check only loaded datamodels to find the dirty ones
            for (DataModel dm : docModel.getDataModelsCollection()) { // only loaded
                if (!dm.isDirty()) {
                    continue;
                }
                DocumentPart part = ((DataModelImpl) dm).getDocumentPart();
                for (Property prop : part.getChildren()) {
                    filterProperty(prop, docModel);
                }
            }
        }
    }

    private void filterProperty(Property prop, DocumentModel docModel) {
        if (!prop.isDirty()) {
            return;
        }
        if (prop instanceof StringProperty) {
            String p = (String) prop.getValue();
            if (p != null && charsToRemove.matchesAnyOf(p)) {
                String filteredProp = filter(p);
                docModel.setPropertyValue(prop.getXPath(), filteredProp);
            }
        } else if (prop instanceof ArrayProperty) {
            Serializable value = prop.getValue();
            if (value instanceof Object[]) {
                Object[] arrayProp = (Object[]) value;
                boolean modified = false;
                for (int i = 0; i < arrayProp.length; i++) {
                    if (arrayProp[i] instanceof String) {
                        String p = (String) arrayProp[i];
                        if (charsToRemove.matchesAnyOf(p)) {
                            arrayProp[i] = filter(p);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    docModel.setPropertyValue(prop.getXPath(), arrayProp);
                }
            }
        } else if (prop instanceof ComplexProperty) {
            ComplexProperty complexProp = (ComplexProperty) prop;
            for (Property subProp : complexProp.getChildren()) {
                filterProperty(subProp, docModel);
            }
        } else if (prop instanceof ListProperty) {
            ListProperty listProp = (ListProperty) prop;
            for (Property p : listProp) {
                filterProperty(p, docModel);
            }
        }
    }
}
