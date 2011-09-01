/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.query.sql.parser;

import java_cup.runtime.*;
import org.nuxeo.ecm.core.query.*;

/**
 * Lexer for NXQL
 */

%%



%class Scanner
%unicode
%char
%cup
%public



%{
  StringBuffer string = new StringBuffer();

  private Symbol symbol(int type) {
    return new Symbol(type, -1, yychar, yytext());
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, -1, yychar, value);
  }

  private void scanError() throws QueryParseException {
    StringBuffer buf = new StringBuffer("Lexical Error: Illegal character <")
        .append(yytext()).append("> at offset ").append(yychar);
      throw new QueryParseException(buf.toString());
  }

%}


LineTerminator = \r|\n|\r\n
WhiteSpace     = {LineTerminator} | [ \t\f]

/*
An Identifier is used for names used for:
variables, fields, tables, functions
 */
/*IdentifierLetter = [:jletterdigit:] | \.*/
Identifier = [:jletter:] [a-zA-Z0-9_:.-]*
FromIdentifier = {Identifier} (\. {Identifier})*
PathIdentifier = {Identifier} (\/ {IdentifierOrIndex})+
IdentifierOrIndex = {Identifier} | {Index} | {Identifier} \[ {Index} \]
Index = {DecIntegerLiteral} | \* | \* {DecIntegerLiteral}


/* integer literal */
DecIntegerLiteral = 0 | [1-9][0-9]*
/* double literal */

DoubleLiteral = ({FLit1}|{FLit2}|{FLit3}) {Exponent}?
FLit1    = [0-9]+ \. [0-9]*
FLit2    = \. [0-9]+
FLit3    = [0-9]+
Exponent = [eE] [+-]? [0-9]+



%state SQ_STRING
%state STRING


%%

/* keywords */
<YYINITIAL> {

    /* White spaces */
    {WhiteSpace}        { /* ignore */ }

    /* SQL Keywords */

    "SELECT"            { return symbol(sym.SELECT); }
    "Select"            { return symbol(sym.SELECT); }
    "select"            { return symbol(sym.SELECT); }

    "FROM"              { return symbol(sym.FROM); }
    "From"              { return symbol(sym.FROM); }
    "from"              { return symbol(sym.FROM); }

    "WHERE"             { return symbol(sym.WHERE); }
    "Where"             { return symbol(sym.WHERE); }
    "where"             { return symbol(sym.WHERE); }

    "AS"                { return symbol(sym.AS); }
    "As"                { return symbol(sym.AS); }
    "as"                { return symbol(sym.AS); }

    "NOT"               { return symbol(sym.NOT); }
    "Not"               { return symbol(sym.NOT); }
    "not"               { return symbol(sym.NOT); }

    "AND"               { return symbol(sym.AND); }
    "And"               { return symbol(sym.AND); }
    "and"               { return symbol(sym.AND); }

    "OR"                { return symbol(sym.OR); }
    "Or"                { return symbol(sym.OR); }
    "or"                { return symbol(sym.OR); }

    "IS"                { return symbol(sym.IS); }
    "Is"                { return symbol(sym.IS); }
    "is"                { return symbol(sym.IS); }

    "NULL"              { return symbol(sym.NULL); }
    "Null"              { return symbol(sym.NULL); }
    "null"              { return symbol(sym.NULL); }

    "LIKE"              { return symbol(sym.LIKE); }
    "Like"              { return symbol(sym.LIKE); }
    "like"              { return symbol(sym.LIKE); }

    "ILIKE"             { return symbol(sym.ILIKE); }
    "ILike"             { return symbol(sym.ILIKE); }
    "ilike"             { return symbol(sym.ILIKE); }

    "DISTINCT"          { return symbol(sym.DISTINCT); }
    "Distinct"          { return symbol(sym.DISTINCT); }
    "distinct"          { return symbol(sym.DISTINCT); }

    "ALL"               { return symbol(sym.ALL); }
    "All"               { return symbol(sym.ALL); }
    "all"               { return symbol(sym.ALL); }

    "BETWEEN"           { return symbol(sym.BETWEEN); }
    "Between"           { return symbol(sym.BETWEEN); }
    "between"           { return symbol(sym.BETWEEN); }

    "IN"                { return symbol(sym.IN); }
    "In"                { return symbol(sym.IN); }
    "in"                { return symbol(sym.IN); }

    "ORDER BY"          { return symbol(sym.ORDERBY); }
    "Order By"          { return symbol(sym.ORDERBY); }
    "order by"          { return symbol(sym.ORDERBY); }

    "ASC"               { return symbol(sym.ASC); }
    "Asc"               { return symbol(sym.ASC); }
    "asc"               { return symbol(sym.ASC); }

    "DESC"              { return symbol(sym.DESC); }
    "Desc"              { return symbol(sym.DESC); }
    "desc"              { return symbol(sym.DESC); }

    /*rich literals*/
    "DATE"              { return symbol(sym.DATE); }
    "Date"              { return symbol(sym.DATE); }
    "date"              { return symbol(sym.DATE); }

    "TIMESTAMP"         { return symbol(sym.TIMESTAMP); }
    "TimeStamp"         { return symbol(sym.TIMESTAMP); }
    "Timestamp"         { return symbol(sym.TIMESTAMP); }
    "timestamp"         { return symbol(sym.TIMESTAMP); }

    "GROUP BY"          { return symbol(sym.GROUPBY); }
    "Group By"          { return symbol(sym.GROUPBY); }
    "group by"          { return symbol(sym.GROUPBY); }

    "HAVING"            { return symbol(sym.HAVING); }
    "Having"            { return symbol(sym.HAVING); }
    "having"            { return symbol(sym.HAVING); }

    "LIMIT"             { return symbol(sym.LIMIT); }
    "Limit"             { return symbol(sym.LIMIT); }
    "limit"             { return symbol(sym.LIMIT); }

    "OFFSET"            { return symbol(sym.OFFSET); }
    "Offset"            { return symbol(sym.OFFSET); }
    "offset"            { return symbol(sym.OFFSET); }

    "TYPE"              { return symbol(sym.TYPE); }
    "Type"              { return symbol(sym.TYPE); }
    "type"              { return symbol(sym.TYPE); }

    "LOCATION"          { return symbol(sym.LOCATION); }
    "Location"          { return symbol(sym.LOCATION); }
    "location"          { return symbol(sym.LOCATION); }

    "STARTSWITH"        { return symbol(sym.STARTSWITH); }
    "StartsWith"        { return symbol(sym.STARTSWITH); }
    "Startswith"        { return symbol(sym.STARTSWITH); }
    "startswith"        { return symbol(sym.STARTSWITH); }

    /* Operators */
    "+"                 { return symbol(sym.SUM); }
    "-"                 { return symbol(sym.SUB); }
    "*"                 { return symbol(sym.MUL); }
    "/"                 { return symbol(sym.DIV); }

    /* Logical operators */
    "="                 { return symbol(sym.EQ); }
    "!="                { return symbol(sym.NOTEQ); }
    "<>"                { return symbol(sym.NOTEQ); }
    "<"                 { return symbol(sym.LT); }
    ">"                 { return symbol(sym.GT); }
    "<="                { return symbol(sym.LTEQ); }
    ">="                { return symbol(sym.GTEQ); }

    /* Symbols */
    "("                 { return symbol(sym.LPARA); }
    ")"                 { return symbol(sym.RPARA); }
    ","                 { return symbol(sym.COMMA); }

    /* String literals */
    \"                  { string.setLength(0); yybegin(STRING); }
    "'"                 { string.setLength(0); yybegin(SQ_STRING); }

    /* Numeric literals */
    {DecIntegerLiteral} { return symbol(sym.INTEGER, yytext()); }
    {DoubleLiteral}     { return symbol(sym.DOUBLE, yytext()); }

    /* Identifiers */
    {Identifier}        { return symbol(sym.IDENTIFIER, yytext()); }
    {PathIdentifier}    { return symbol(sym.PATH_IDENTIFIER, yytext()); }
    {FromIdentifier}    { return symbol(sym.FROM_IDENTIFIER, yytext()); }

}

<STRING> {
    \"                  { yybegin(YYINITIAL);
                          return symbol(sym.STRING, string.toString());
                        }
    [^\n\r\"\\]+        { string.append( yytext() ); }
    \\t                 { string.append('\t'); }
    \\n                 { string.append('\n'); }
    \\r                 { string.append('\r'); }
    \\\"                { string.append('\"'); }
    \\                  { string.append('\\'); }
}

<SQ_STRING> {
    \'                  { yybegin(YYINITIAL);
                          return symbol(sym.STRING,string.toString());
                        }
    [^\n\r\'\\]+        { string.append( yytext() ); }
    \\t                 { string.append('\t'); }
    \\n                 { string.append('\n'); }
    \\r                 { string.append('\r'); }
    \\\'                { string.append('\''); }
    \\                  { string.append('\\'); }
}



/* error fallback */
.|\n                    { scanError(); }
