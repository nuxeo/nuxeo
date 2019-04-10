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

package org.nuxeo.dam.importer.core.filter;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.nuxeo.ecm.platform.importer.filter.ImportingDocumentFilter;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * Filter not wanted files and folders during the import.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DamImportingDocumentFilter implements ImportingDocumentFilter {

    protected static final List<Pattern> IGNORE_PATTERNS = Arrays.asList(
            Pattern.compile("__MACOSX"), Pattern.compile("\\.DS_Store"));

    public boolean shouldImportDocument(SourceNode sourceNode) {
        for (Pattern pattern : IGNORE_PATTERNS) {
            if (pattern.matcher(sourceNode.getName()).matches()) {
                return false;
            }
        }
        return true;
    }

}
