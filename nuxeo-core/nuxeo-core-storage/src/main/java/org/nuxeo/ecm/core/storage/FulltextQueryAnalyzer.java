/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.query.QueryParseException;

/**
 * Structured fulltext query analyzer.
 */
public class FulltextQueryAnalyzer {

    protected static final String SPACE = " ";

    protected static final String PLUS = "+";

    protected static final String MINUS = "-";

    protected static final char CSPACE = ' ';

    protected static final String DOUBLE_QUOTES = "\"";

    protected static final String OR = "OR";

    protected static final Pattern SEPARATOR = Pattern.compile("[ ]");

    protected static final Pattern IGNORED = Pattern.compile("\\p{Punct}+");

    /**
     * Structured fulltext query operator.
     */
    public enum Op {
        OR, AND, WORD, NOTWORD
    }

    /**
     * Structured fulltext query.
     */
    public static class FulltextQuery {

        public Op op;

        /** The list of terms, if op is OR or AND */
        public List<FulltextQuery> terms;

        /** The word, if op is WORD or NOTWORD */
        public String word;

        /**
         * Checks if the word is a phrase.
         */
        public boolean isPhrase() {
            return word != null && word.contains(SPACE);
        }
    }

    protected FulltextQuery ft = new FulltextQuery();

    protected List<FulltextQuery> terms = new LinkedList<>();

    protected FulltextQuery analyze(String query) {
        query = query.replaceAll(" +", " ").trim();
        if (query.trim().length() == 0) {
            return null;
        }
        ft.op = Op.OR;
        ft.terms = new LinkedList<>();
        // current sequence of ANDed terms
        boolean wasOr = false;
        String[] words = split(query);
        for (Iterator<String> it = Arrays.asList(words).iterator(); it.hasNext();) {
            boolean plus = false;
            boolean minus = false;
            String word = it.next();
            if (ignored(word)) {
                continue;
            }
            if (word.startsWith(PLUS)) {
                plus = true;
                word = word.substring(1);
            } else if (word.startsWith(MINUS)) {
                minus = true;
                word = word.substring(1);
            }
            if (word.startsWith(DOUBLE_QUOTES)) {
                // read phrase
                word = word.substring(1);
                StringBuilder phrase = null;
                while (true) {
                    boolean end = word.endsWith(DOUBLE_QUOTES);
                    if (end) {
                        word = word.substring(0, word.length() - 1).trim();
                    }
                    if (word.contains(DOUBLE_QUOTES)) {
                        throw new QueryParseException("Invalid fulltext query (double quotes in word): " + query);
                    }
                    if (word.length() != 0) {
                        if (phrase == null) {
                            phrase = new StringBuilder();
                        } else {
                            phrase.append(CSPACE);
                        }
                        phrase.append(word);
                    }
                    if (end) {
                        break;
                    }
                    if (!it.hasNext()) {
                        throw new QueryParseException("Invalid fulltext query (unterminated phrase): " + query);
                    }
                    word = it.next();
                }
                if (phrase == null) {
                    continue;
                }
                word = phrase.toString();
            } else if (word.equalsIgnoreCase(OR)) {
                if (wasOr) {
                    throw new QueryParseException("Invalid fulltext query (OR OR): " + query);
                }
                if (terms.isEmpty()) {
                    throw new QueryParseException("Invalid fulltext query (standalone OR): " + query);
                }
                wasOr = true;
                continue;
            }
            FulltextQuery w = new FulltextQuery();
            if (minus) {
                if (word.length() == 0) {
                    throw new QueryParseException("Invalid fulltext query (standalone -): " + query);
                }
                w.op = Op.NOTWORD;
            } else {
                if (plus) {
                    if (word.length() == 0) {
                        throw new QueryParseException("Invalid fulltext query (standalone +): " + query);
                    }
                }
                w.op = Op.WORD;
            }
            if (wasOr) {
                endAnd();
                wasOr = false;
            }
            w.word = word;
            terms.add(w);
        }
        if (wasOr) {
            throw new QueryParseException("Invalid fulltext query (final OR): " + query);
        }
        // final terms
        endAnd();
        int size = ft.terms.size();
        if (size == 0) {
            // all terms were negative
            return null;
        } else if (size == 1) {
            // simplify when no OR
            ft = ft.terms.get(0);
        }
        return ft;
    }

    protected String[] split(String query) {
        return SEPARATOR.split(query);
    }

    protected boolean ignored(String word) {
        if ("-".equals(word) || "+".equals(word) || word.contains("\"")) {
            return false; // dealt with later, different error
        }
        return IGNORED.matcher(word).matches();
    }

    // add current ANDed terms to global OR
    protected void endAnd() {
        // put negative words at the end
        List<FulltextQuery> pos = new LinkedList<>();
        List<FulltextQuery> neg = new LinkedList<>();
        for (FulltextQuery term : terms) {
            if (term.op == Op.NOTWORD) {
                neg.add(term);
            } else {
                pos.add(term);
            }
        }
        if (!pos.isEmpty()) {
            terms = pos;
            terms.addAll(neg);
            if (terms.size() == 1) {
                ft.terms.add(terms.get(0));
            } else {
                FulltextQuery a = new FulltextQuery();
                a.op = Op.AND;
                a.terms = terms;
                ft.terms.add(a);
            }
        }
        terms = new LinkedList<>();
    }

    public static void translate(FulltextQuery ft, StringBuilder sb, String or, String and, String andNot,
            String wordStart, String wordEnd, Set<Character> wordCharsReserved, String phraseStart, String phraseEnd,
            boolean quotePhraseWords) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            sb.append('(');
            for (int i = 0; i < ft.terms.size(); i++) {
                FulltextQuery term = ft.terms.get(i);
                if (i > 0) {
                    sb.append(' ');
                    if (ft.op == Op.OR) {
                        sb.append(or);
                    } else { // Op.AND
                        if (term.op == Op.NOTWORD) {
                            sb.append(andNot);
                        } else {
                            sb.append(and);
                        }
                    }
                    sb.append(' ');
                }
                translate(term, sb, or, and, andNot, wordStart, wordEnd, wordCharsReserved, phraseStart, phraseEnd,
                        quotePhraseWords);
            }
            sb.append(')');
            return;
        } else {
            String word = ft.word;
            if (ft.isPhrase()) {
                if (quotePhraseWords) {
                    boolean first = true;
                    for (String w : word.split(" ")) {
                        if (!first) {
                            sb.append(" ");
                        }
                        first = false;
                        appendWord(w, sb, wordStart, wordEnd, wordCharsReserved);
                    }
                } else {
                    sb.append(phraseStart);
                    sb.append(word);
                    sb.append(phraseEnd);
                }
            } else {
                appendWord(word, sb, wordStart, wordEnd, wordCharsReserved);
            }
        }
    }

    protected static void appendWord(String word, StringBuilder sb, String start, String end, Set<Character> reserved) {
        boolean quote = true;
        if (!reserved.isEmpty()) {
            for (char c : word.toCharArray()) {
                if (reserved.contains(Character.valueOf(c))) {
                    quote = false;
                    break;
                }
            }
        }
        if (quote) {
            sb.append(start);
        }
        sb.append(word);
        if (quote) {
            sb.append(end);
        }
    }

    public static boolean hasPhrase(FulltextQuery ft) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            for (FulltextQuery term : ft.terms) {
                if (hasPhrase(term)) {
                    return true;
                }
            }
            return false;
        } else {
            return ft.isPhrase();
        }
    }

    /**
     * Analyzes a fulltext query into a generic datastructure that can be used for each specific database.
     * <p>
     * List of terms containing only negative words are suppressed. Otherwise negative words are put at the end of the
     * lists of terms.
     */
    public static FulltextQuery analyzeFulltextQuery(String query) {
        return new FulltextQueryAnalyzer().analyze(query);
    }

    /**
     * Translate fulltext into a common pattern used by many servers.
     */
    public static String translateFulltext(FulltextQuery ft, String or, String and, String andNot, String phraseQuote) {
        StringBuilder sb = new StringBuilder();
        translate(ft, sb, or, and, andNot, "", "", Collections.<Character> emptySet(), phraseQuote, phraseQuote, false);
        return sb.toString();
    }

    /**
     * Translate fulltext into a common pattern used by many servers.
     */
    public static String translateFulltext(FulltextQuery ft, String or, String and, String andNot, String wordStart,
            String wordEnd, Set<Character> wordCharsReserved, String phraseStart, String phraseEnd,
            boolean quotePhraseWords) {
        StringBuilder sb = new StringBuilder();
        translate(ft, sb, or, and, andNot, wordStart, wordEnd, wordCharsReserved, phraseStart, phraseEnd,
                quotePhraseWords);
        return sb.toString();
    }

}
