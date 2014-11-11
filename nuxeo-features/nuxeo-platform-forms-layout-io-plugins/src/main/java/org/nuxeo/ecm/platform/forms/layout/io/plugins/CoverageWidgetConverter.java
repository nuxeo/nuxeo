/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.io.plugins;

import java.util.Arrays;
import java.util.List;

/**
 * Converter for the default "coverage" widget that uses a vocabulary
 *
 * @since 5.5
 */
public class CoverageWidgetConverter extends
        AbstractChainedVocabularyWidgetConverter {

    @Override
    protected List<String> getAcceptedWidgetNames() {
        return Arrays.asList(new String[] { "coverage" });
    }

    @Override
    protected String getParentDirectoryName() {
        return "continent";
    }

    @Override
    protected String getChildDirectoryName() {
        return "country";
    }

}
