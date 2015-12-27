/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.io.plugins;

import java.util.Arrays;
import java.util.List;

/**
 * Converter for the default "subjects" widget that uses a vocabulary
 *
 * @since 5.5
 */
public class SubjectsWidgetConverter extends AbstractChainedVocabularyWidgetConverter {

    @Override
    protected List<String> getAcceptedWidgetNames() {
        return Arrays.asList(new String[] { "subjects" });
    }

    @Override
    protected String getParentDirectoryName() {
        return "topic";
    }

    @Override
    protected String getChildDirectoryName() {
        return "subtopic";
    }

}
