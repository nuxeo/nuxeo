/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import com.google.common.base.CharMatcher;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.Collection;
import java.util.List;

/**
 * @since 9.1
 */
public class CharacterFilteringServiceImpl extends DefaultComponent implements CharacterFilteringService {

    private static final Log log = LogFactory.getLog(CharacterFilteringServiceImpl.class);

    public static final String FILTERING_XP = "filtering";

    protected CharacterFilteringServiceDescriptor desc;

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (FILTERING_XP.equals(point)) {
            desc = (CharacterFilteringServiceDescriptor) contrib;
        } else {
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public boolean isFilteringEnabled() {
        return desc.isEnabled();
    }

    @Override
    public void filterChars(DocumentModel docModel) {
        CharMatcher charsToPreserve = CharMatcher.anyOf("\r\n\t");
        CharMatcher allButPreserved = charsToPreserve.negate();
        CharMatcher controlCharactersToRemove = CharMatcher.JAVA_ISO_CONTROL.and(allButPreserved);

        List<String> additionalChars = desc.getUnallowedChars();
        String otherCharsToRemove = "";
        if (additionalChars != null && !additionalChars.isEmpty()) {
            for (String c : additionalChars) {
                otherCharsToRemove += StringEscapeUtils.unescapeJava(c);
            }
            controlCharactersToRemove = controlCharactersToRemove.or(CharMatcher.anyOf(otherCharsToRemove));
        }

        String[] schemas = docModel.getSchemas();
        for (String schema : schemas) {
            Collection<Property> properties = docModel.getPropertyObjects(schema);
            for (Property prop : properties) {
                filterProperty(controlCharactersToRemove, prop, docModel, schema);
            }
        }
    }

    private void filterProperty(CharMatcher controlChars, Property prop, DocumentModel docModel, String schema) {
        if (prop instanceof StringProperty && prop.getValue() != null) {
            String filteredProp = controlChars.removeFrom((String) prop.getValue());
            docModel.setProperty(schema, prop.getName(), filteredProp);
        } else if (prop instanceof ComplexProperty) {
            ComplexProperty complexProp = (ComplexProperty) prop;
            for (Property subProp : complexProp.getChildren()) {
                filterProperty(controlChars, subProp, docModel, schema);
            }
        }
    }
}
