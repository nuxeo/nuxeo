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
 *     Based on source code for LeterTokenizer by the Apache Software Foundation
 *
 * $Id: CompassBackend.java 29926 2008-02-06 18:56:29Z tdelprat $
 */

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

/** Nuxeo French Tokenizer.
*
* This tokenizer will cut at anything that is nor a letter, nor a digit.
 */

public class FrenchTokenizer extends CharTokenizer {
  /** Construct a new LetterTokenizer. */
  public FrenchTokenizer(Reader in) {
    super(in);
  }

  /** Collects only characters which satisfy
   * {@link Character#isLetterOrDigit(char)}.*/
  protected boolean isTokenChar(char c) {
    return Character.isLetterOrDigit(c);
  }
}
