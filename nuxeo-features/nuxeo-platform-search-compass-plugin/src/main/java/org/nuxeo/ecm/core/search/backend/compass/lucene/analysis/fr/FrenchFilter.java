/* (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass.lucene.analysis.fr;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizerConstants;

public class FrenchFilter extends TokenFilter implements StandardTokenizerConstants {

	private static final Log log = LogFactory.getLog(FrenchFilter.class);

	private static final String APOSTROPHE_TYPE = tokenImage[APOSTROPHE];

	public FrenchFilter(TokenStream in) {
	    super(in);
	}

	public Token next() throws IOException {
		Token token = input.next();
		if( token == null ) return null;


		String text = token.termText();
		String type = token.type();

		// exagerated log
		log.debug( "text = " + text + " type =" + type );

		if ( type == APOSTROPHE_TYPE ){
			String loText = text.toLowerCase();
			if (loText.startsWith("l'") || text.startsWith("d'")
					|| loText.startsWith("n'") || text.startsWith("m'")
					|| loText.startsWith("s'") || text.startsWith("t'")
					|| loText.startsWith("c'") || text.startsWith("j'")) {
				return new Token(text.substring(2), token.startOffset(), token.endOffset(), type);
			}
			if (loText.startsWith("qu'")) {
				return new Token(text.substring(3), token.startOffset(), token.endOffset(), type);
			}
			if (loText.endsWith("'s")) {
				return new Token(text.substring(0,text.length()-2), token.startOffset(), token.endOffset(), type);
			}
		}
		return token;
	}

}
