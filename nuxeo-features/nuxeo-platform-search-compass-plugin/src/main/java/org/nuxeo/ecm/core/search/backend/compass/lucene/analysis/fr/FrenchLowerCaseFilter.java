package org.nuxeo.ecm.core.search.backend.compass.lucene.analysis.fr;

/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Georges Racinet <georges@racinet.fr> Implementation
 *     Based on source code for LowercaseFilter by the Apache Software Foundation
 *
 * $Id: CompassBackend.java 29926 2008-02-06 18:56:29Z tdelprat $
 */

import java.io.IOException;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.nuxeo.common.utils.StringUtils;

/**
 * Normalizes token text to lower case.
 *
 * @version $Id: LowerCaseFilter.java 150259 2004-03-29 22:48:07Z cutting $
 */
public final class FrenchLowerCaseFilter extends TokenFilter {
  public FrenchLowerCaseFilter(TokenStream in) {
    super(in);
  }

  public final Token next() throws IOException {
    Token token = input.next();

    if (token == null)
      return null;

    String term = StringUtils.toAscii(token.termText()).toLowerCase();
    return new Token(term, token.startOffset(), token.endOffset(), token.type());

  }
}
