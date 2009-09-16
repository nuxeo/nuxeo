/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
/**
 * CMISQL tree grammar walker, used as a Nuxeo QueryMaker.
 */
tree grammar NuxeoCmisWalker;

options {
    tokenVocab = CmisSqlParser;
    ASTLabelType = CommonTree;
    output = AST;
}

@header {
/*
 * THIS FILE IS AUTO-GENERATED, DO NOT EDIT.
 *
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *
 * THIS FILE IS AUTO-GENERATED, DO NOT EDIT.
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.sql.db.Column;
}

@members {
    public CMISQLQueryMaker queryMaker;
    public String sql;
    /** columns referenced, keyed by full quoted name (includes alias name) */
    public Map<String, Column> columns = new HashMap<String, Column>();
    /** qualifier to set of columns full quoted names */
    public Map<String, Set<String>> columnsPerQual = new HashMap<String, Set<String>>();
    /** original column names as specified in query */
    public Map<String, String> columnsSpecified = new HashMap<String, String>();

    public List<String> select_what = new LinkedList<String>();

    public static class Join {
        String kind;
        String table;
        String corr;
        String on1;
        String on2;
    }
    /** without kind or on */
    public List<Join> from_joins = new LinkedList<Join>();

    public String select_where;

    public List<String> select_orderby = new LinkedList<String>();

    public String referToColumn(String c, String qual) {
        Column col = queryMaker.findColumn(c, qual);
        String fqn = col.getFullQuotedName();
        columns.put(fqn, col);
        columnsSpecified.put(fqn, queryMaker.getCanonicalColumnName(c, qual));
        Set<String> set = columnsPerQual.get(qual);
        if (set == null) {
            columnsPerQual.put(qual, set = new HashSet<String>());
        }
        set.add(fqn);
        return fqn;
    }
}

query [CMISQLQueryMaker qm]
@init {
    queryMaker = $qm;
}:
    ^(SELECT select_list from_clause where_clause order_by_clause?)
    ;

select_list
@init {
    List<String> sqls = new ArrayList<String>();
}:
      STAR
        {
            select_what.add(referToColumn("cmis:ObjectId", null)); // TODO
        }
    | ^(LIST (select_sublist { select_what.add($select_sublist.sql); })+)
    ;

select_sublist returns [String sql]:
      value_expression column_name?
        {
            $sql = $value_expression.sql;
        }
    | qualifier DOT STAR
        {
            $sql = referToColumn("cmis:ObjectId", $qualifier.qual); // TODO
        }
    ;

value_expression returns [String sql]:
      column_reference
        {
            $sql = $column_reference.sql;
        }
//    | string_value_function
//    | numeric_value_function
    ;

column_reference returns [String sql]:
    ^(COL qualifier? column_name)
      {
          String c = $column_name.start.getText();
          String qual = $qualifier.qual;
          $sql = referToColumn(c, qual);
      }
    ;

qualifier returns [String qual]:
      table_name
        {
            $qual = $table_name.text;
        }
//    | correlation_name
    ;

from_clause:
    ^(FROM table_reference)
    ;

table_reference:
    one_table
      {
          from_joins.add($one_table.join);
      }
    // continued:
    (table_join
      {
          from_joins.add($table_join.join);
      }
    )*
    ;

table_join returns [Join join]:
    ^(JOIN join_kind one_table join_specification?)
      {
          Join join = $one_table.join;
          join.kind = $join_kind.text;
          join.on1 = $join_specification.col1;
          join.on2 = $join_specification.col2;
          $join = join;
      }
    ;

one_table returns [Join join]:
    ^(TABLE table_name correlation_name?)
      {
          Join join = new Join();
          join.table = $table_name.text;
          join.corr = $correlation_name.text;
          $join = join;
      }
    ;

join_kind:
    INNER | LEFT | RIGHT;

join_specification returns [String col1, String col2]:
    ^(ON c1=column_reference EQ c2=column_reference)
      {
          $col1 = $c1.sql;
          $col2 = $c2.sql;
      }
    ;

where_clause returns [String sql]:
      ^(WHERE search_condition)
        {
            select_where = $search_condition.sql;
        }
    | /* nothing */
        {
            select_where = null;
        }
    ;

search_condition returns [String sql]
@init {
    List<String> sqls = new ArrayList<String>();
}:
      boolean_term
        {
            $sql = $boolean_term.sql;
        }
    | ^(OR (boolean_term {sqls.add($boolean_term.sql);})+)
        {
            $sql = StringUtils.join(sqls, " OR ");
        }
    ;

boolean_term returns [String sql]
@init {
    List<String> sqls = new ArrayList<String>();
}:
      boolean_factor
        {
            $sql = $boolean_factor.sql;
        }
    | ^(AND (boolean_factor {sqls.add($boolean_factor.sql);})+)
        {
            $sql = StringUtils.join(sqls, " AND ");
        }
    ;

boolean_factor returns [String sql]:
      boolean_test
        {
            $sql = $boolean_test.sql;
        }
    | ^(NOT boolean_test)
        {
            $sql = "(NOT " + $boolean_test.sql + ")";
        }
    ;

boolean_test returns [String sql]:
      predicate
        {
            $sql = $predicate.sql;
        }
//    | search_condition
    ;

predicate returns [String sql]:
      ^(UN_OP un_op un_arg)
        {
            int token = $un_op.start.getType();
            switch (token) {
                case IS_NULL:
                    $sql = $un_arg.sql + " IS NULL";
                    break;
                case IS_NOT_NULL:
                    $sql = $un_arg.sql + " IS NOT NULL";
                    break;
                default:
                    throw new UnwantedTokenException(token, input);
            }
        }
    | ^(BIN_OP bin_op arg1=bin_arg arg2=bin_arg)
        {
            int token = $bin_op.start.getType();
            String op;
            switch (token) {
                case EQ:
                    op = "=";
                    break;
                case NEQ:
                    op = "<>";
                    break;
                default:
                    throw new UnwantedTokenException(token, input);
            }
            $sql = "(" + $arg1.sql + " " + op + " " + $arg2.sql + ")";
        }
//    | text_search_predicate
//    | folder_predicate
    ;

un_op:
    IS_NULL | IS_NOT_NULL;

un_arg returns [String sql]:
    column_reference
      {
          $sql = $column_reference.sql;
      }
    ;

bin_op:
    EQ | NEQ | LT | GT | LTEQ | GTEQ | LIKE | NOT_LIKE;

bin_arg returns [String sql]
@init {
    List<String> sqls = new ArrayList<String>();
}:
      value_expression
        {
            $sql = $value_expression.sql;
        }
    | literal
        {
            $sql = $literal.sql;
        }
    | ^(LIST (literal {sqls.add($literal.sql);})+)
        {
            $sql = StringUtils.join(sqls, ", ");
        }
    ;

literal returns [String sql]:
      NUM_LIT
        {
            $sql = Long.valueOf($NUM_LIT.text).toString();
        }
    | STRING_LIT
        {
            String s = $STRING_LIT.text;
            s = s.substring(1, s.length() - 1);
            // TODO use query parameters to avoid injection attacks
            // escape SQL string
            s = s.replace("'", "''").replace("\\", "\\\\");
            $sql = "'" + s + "'";
        }
    ;

order_by_clause:
    ^(ORDER_BY sort_specification+)
    ;

sort_specification:
    column_reference ( o=ASC | o=DESC )
      {
          String col = $column_reference.sql;
          if ($o.type == DESC) {
              col += " DESC";
          }
          select_orderby.add(col);
      }
    ;

correlation_name:
    ID;

table_name:
    ID;

column_name:
    ID;

multi_valued_column_name:
    ID;
