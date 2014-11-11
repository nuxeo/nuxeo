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

/**
 * Copyright 2004-2005 The Apache Software Foundation
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
 */

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;

/**
 * A filter that stemms french words. It supports a table of words that should
 * not be stemmed at all. The used stemmer can be changed at runtime after the
 * filter object is created (as long as it is a FrenchStemmer).
 *
 * @author    Patrick Talbot (based on Gerhard Schwarz work for German)
 */
@SuppressWarnings({"ALL"})
public final class FrenchStemFilter extends TokenFilter {

    /**
     * The actual token in the input stream.
     */
    private Token token = null;
    private FrenchStemmer stemmer = null;
    private Set exclusions = null;
    private boolean caseSensitive = false;

    public FrenchStemFilter( TokenStream in ) {
    super(in);
        stemmer = new FrenchStemmer();
    }

    /**
     * Builds a FrenchStemFilter that uses an exclusiontable.
   *
   * @deprecated
     */
    public FrenchStemFilter( TokenStream in, Hashtable exclusiontable ) {
        this( in );
        exclusions = new HashSet(exclusiontable.keySet());
    }

    public FrenchStemFilter( TokenStream in, Set exclusiontable ) {
        this( in );
        exclusions = exclusiontable;
    }

    public FrenchStemFilter( TokenStream in, Set exclusiontable, boolean caseSens ) {
        this( in );
        exclusions = exclusiontable;
        caseSensitive = caseSens;
    }

    /**
     * @return  Returns the next token in the stream, or null at EOS
     */
    public final Token next()
        throws IOException {
        if ( ( token = input.next() ) == null ) {
            return null;
        }
        // Check the exclusiontable
        else if ( exclusions != null && exclusions.contains( caseSensitive?token.termText().toLowerCase():token.termText() ) ) {
            return token;
        }
        else {
            String s = stemmer.stem( caseSensitive?token.termText().toLowerCase():token.termText() );
            // If not stemmed, dont waste the time creating a new token
            if ( !s.equals( caseSensitive?token.termText().toLowerCase():token.termText() ) ) {
               return new Token( s, token.startOffset(), token.endOffset(), token.type());
            }
            return token;
        }
    }
    /**
     * Set a alternative/custom FrenchStemmer for this filter.
     */
    public void setStemmer( FrenchStemmer stemmer ) {
        if ( stemmer != null ) {
            this.stemmer = stemmer;
        }
    }
    /**
     * Set an alternative exclusion list for this filter.
     */
    public void setExclusionTable( Hashtable exclusiontable ) {
        exclusions = new HashSet(exclusiontable.keySet());
    }
}
