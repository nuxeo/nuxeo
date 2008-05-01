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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.security;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PostfixExpression implements Iterable<PostfixExpression.Token> {

    public static final Pattern PATTERN = Pattern.compile("\\(|\\)|NOT|AND|OR");

    public static final int ARG = 0;
    public static final int NOT = 1;
    public static final int AND = 2;
    public static final int OR = 3;
    public static final int PARA = 4;
    public static final int LPARA = 5;
    public static final int RPARA = 6;

    protected Token[] expr;


    public PostfixExpression(String expr) throws ParseException {
        parse(expr);
    }

//    public PostfixExpression(String expr, String ops) throws ParseException {
//        parse(expr);
//    }

    public Token[] getExpression() {
        return expr;
    }

    public Iterator<Token> iterator() {
        return Arrays.asList(expr).iterator();
    }

    private void and(OpStack stack, List<Token> result, String expr, int s, int i) {
        if (s > -1 && s < i) {
            result.add(new Token(ARG, expr.substring(s, i)));
        }
        pushOp(new Token(AND, "AND"), stack, result);
    }

    private void or(OpStack stack, List<Token> result, String expr, int s, int i) {
        if (s > -1 && s < i) {
            result.add(new Token(ARG, expr.substring(s, i)));
        }
        pushOp(new Token(OR, "OR"), stack, result);
    }

    private void not(OpStack stack, List<Token> result, String expr, int s, int i) {
        if (s > -1 && s < i) {
            result.add(new Token(ARG, expr.substring(s, i)));
        }
        pushOp(new Token(NOT, "NOT"), stack, result);
    }

    protected void parse(String expr) throws ParseException {
        char[] chars = expr.toCharArray();
        OpStack stack = new OpStack();
        List<Token> result = new ArrayList<Token>();
        int s = -1;
        boolean space = false;
        for (int i=0; i<chars.length; i++) {
            char c = chars[i];
            switch (c) {
            case 'A':
                if (space && i+3<chars.length) {
                    if (chars[i+1] == 'N' && chars[i+2] == 'D' && Character.isWhitespace(chars[i+3])) {
                        and(stack, result, expr, s, i);
                    }
                    s = -1;
                    i+=3;
                }
                space = false;
                if (s == -1) {
                    s = i; // start new argument
                }
                break;
            case 'O':
                if (space && i+2<chars.length) {
                    if (chars[i+1] == 'R' && Character.isWhitespace(chars[i+2])) {
                        or(stack, result, expr, s, i);
                    }
                    s = -1;
                    i+=2;
                }
                space = false;
                if (s == -1) {
                    s = i; // start new argument
                }
                break;
            case 'N':
                if (space && i+3<chars.length) {
                    if (chars[i+1] == 'O' && chars[i+2] == 'T' && Character.isWhitespace(chars[i+3])) {
                        not(stack, result, expr, s, i);
                    }
                    s = -1;
                    i+=3;
                }
                space = false;
                if (s == -1) {
                    s = i; // start new argument
                }
                break;
            case '(':
                space = false;
                s = -1;
                stack.push(new Token(LPARA, "("));
                break;
            case ')': // pop from stack until first '(' is reached
                space = false;
                if (s > -1 && s<i) {
                    result.add(new Token(ARG, expr.substring(s, i)));
                }
                s = -1;
                while (!stack.isEmpty() && stack.top().type != LPARA) {
                    result.add(stack.pop());
                }
                if (stack.isEmpty()) {
                    throw new ParseException("Not matching LPARA '(' found ", i);
                }
                stack.pop(); // remove LPARA from stack
                break;
            case ' ':
            case '\t':
                space = true;
                if (s > -1 && s<i) {
                    result.add(new Token(ARG, expr.substring(s, i)));
                }
                s = -1;
                break;
            default:
                space = false;
                if (s == -1) {
                    s = i; // start new argument
                }
                break;
            }
        }
        if (s > -1 && s<expr.length()) {
            result.add(new Token(ARG, expr.substring(s)));
        }
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        this.expr = result.toArray(new Token[result.size()]);
    }

    private static void pushOp(Token tok, OpStack stack, List<Token> result) {
        if (!stack.isEmpty() && stack.top().type <= tok.type) {
            result.add(stack.pop());
        }
        stack.push(tok);
    }

    public Object visit(Visitor visitor) {
        LinkedList<Object> stack = new LinkedList<Object>();
        for (Token token : expr) {
            if (token.type == ARG) {
                stack.add(visitor.createParameter(token));
            } else {
                Object lparam;
                Object rparam = null;
                int arity = token.type > NOT ? 2 : 1;
                if (arity == 1) {
                    lparam = stack.removeLast();
                } else {// arity == 2
                    rparam = stack.removeLast();
                    lparam = stack.removeLast();
                }
                stack.add(visitor.createOperation(token, lparam, rparam));
            }
        }
        return stack.getLast();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Token token : expr) {
            sb.append(token.name).append(" ");
        }
        return sb.toString();
    }

    public static interface Visitor {
        Object createParameter(Token token);
        Object createOperation(Token token, Object lparam, Object rparam);
    }

    public static class Token {
        public final int type;
        public final String name;

        public Token(int type, String name) {
            this.type = type;
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    public static class OpStack extends LinkedList<Token> {
        private static final long serialVersionUID = 1L;
        public final void push(Token token) {
            add(token);
        }
        public final Token pop() {
            return removeLast();
        }
        public final Token top() {
            return getLast();
        }
    }

}
