/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.csv.export.validation;

import static org.nuxeo.ecm.core.bulk.action.computation.SortBlob.SORT_PARAMETER;
import static org.nuxeo.ecm.core.bulk.action.computation.ZipBlob.ZIP_PARAMETER;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_LANG;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_SCHEMAS;
import static org.nuxeo.ecm.platform.csv.export.computation.CSVProjectionComputation.PARAM_XPATHS;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.bulk.AbstractBulkActionValidation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;

/**
 * @since 10.10
 */
public class CSVExportValidation extends AbstractBulkActionValidation {

    @Override
    protected List<String> getParametersToValidate() {
        return Arrays.asList(SORT_PARAMETER, ZIP_PARAMETER, PARAM_LANG, PARAM_SCHEMAS, PARAM_XPATHS);
    }

    @Override
    protected void validateCommand(BulkCommand command) throws IllegalArgumentException {

        // Check sort and zip parameters
        validateBoolean(SORT_PARAMETER, command);
        validateBoolean(ZIP_PARAMETER, command);

        // Check for lang parameter and if it is a known language
        validateString(PARAM_LANG, command);
        String lang = command.getParam(PARAM_LANG);
        if (lang != null && !Arrays.asList(Locale.getAvailableLocales()).contains(Locale.forLanguageTag(lang))) {
            throw new IllegalArgumentException(invalidParameterMessage(PARAM_LANG, command));
        }

        // Check schemas parameter and if they exists
        validateList(PARAM_SCHEMAS, command);
        List<?> schemas = command.getParam(PARAM_SCHEMAS);
        if (schemas != null) {
            for (Object schema : schemas) {
                validateSchema(PARAM_SCHEMAS, (Serializable) schema, command);
            }
        }

        // Check xpaths parameter and if they exists
        validateList(PARAM_XPATHS, command);
        List<?> xpaths = command.getParam(PARAM_XPATHS);
        if (xpaths != null) {
            for (Object xpath : xpaths) {
                validateXpath(PARAM_XPATHS, (Serializable) xpath, command);
            }
        }
    }
}
