/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.wiki.listener;

import org.wikimodel.wem.PrintListener;
import org.wikimodel.wem.WikiPrinter;

public class WordExtractor extends PrintListener {

    protected final StringBuilder words = new StringBuilder();

    final StringBuffer collector;

    public WordExtractor(StringBuffer collector) {
        super(new WikiPrinter());
        this.collector = collector;
    }

    @Override
    public void onWord(String str) {
        if (collector != null) {
            collector.append(str);
        }
    }

    @Override
    public void onSpecialSymbol(String str) {
        if (collector == null) {
            return;
        }
        if (".".equals(str)) {
            collector.append(str);
        } else {
            collector.append(" ");
        }
    }

    @Override
    public void onSpace(String str) {
        if (collector != null) {
            collector.append(str);
        }
    }

    @Override
    public void onEmptyLines(int count) {
        if (collector != null) {
            collector.append(" ");
        }
    }

    @Override
    public void onNewLine() {
        if (collector != null) {
            collector.append(" ");
        }
    }

}
