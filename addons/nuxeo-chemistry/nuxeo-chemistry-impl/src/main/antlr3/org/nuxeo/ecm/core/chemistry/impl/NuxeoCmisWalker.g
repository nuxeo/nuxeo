/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.chemistry.Property;
import org.apache.chemistry.util.GregorianCalendar;
import org.nuxeo.ecm.core.chemistry.impl.CMISQLQueryMaker.Join;
import org.nuxeo.ecm.core.chemistry.impl.CMISQLQueryMaker.SelectedColumn;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
}

@members {
    public CMISQLQueryMaker queryMaker;

    public boolean select_distinct;

    public List<SelectedColumn> select_what = new LinkedList<SelectedColumn>();

    /** without kind or on */
    public List<Join> from_joins = new LinkedList<Join>();

    public String select_where;

    public List<Serializable> select_where_params;

    public List<String> select_orderby = new LinkedList<String>();

    public int multiref = 1;

    @Override
    public void displayRecognitionError(String[] tokenNames,
            RecognitionException e) {
        queryMaker.errorMessages.add(getErrorMessage(e, tokenNames));
    }
}

query [CMISQLQueryMaker qm]
@init {
    queryMaker = $qm;
}:
    ^(SELECT DISTINCT? select_list from_clause where_clause order_by_clause?)
      {
          select_distinct = $DISTINCT != null;
      }
    ;

select_list
@init {
    List<String> sqls = new ArrayList<String>();
}:
      STAR
        {
            select_what.add(queryMaker.referToAllColumns(null));
        }
    | ^(LIST (s=select_sublist { select_what.add($s.selcol); })+)
    ;

select_sublist returns [SelectedColumn selcol]:
      v=select_value_expression column_name?
        {
            $selcol = $v.selcol;
            if ($column_name.start != null) {
                String alias = $column_name.start.getText();
                queryMaker.aliasColumn($selcol, alias);
            }
        }
    | qualifier DOT STAR
        {
            $selcol = queryMaker.referToAllColumns($qualifier.qual);
        }
//    | select_multi_valued_column_reference TODO
    ;

select_value_expression returns [SelectedColumn selcol]:
      c=select_column_reference { $selcol = $c.selcol; }
    | v=numeric_value_function { $selcol = $v.selcol; }
    ;

numeric_value_function returns [SelectedColumn selcol]:
    ^(FUNC SCORE)
      {
          $selcol = queryMaker.referToScoreInSelect();
      }
    ;

select_column_reference returns [SelectedColumn selcol]:
    ^(COL qualifier? column_name)
      {
          String c = $column_name.start.getText();
          String qual = $qualifier.qual;
          $selcol = queryMaker.referToColumnInSelect(c, qual);
      }
    ;

qualifier returns [String qual, List<Serializable> params]:
      table_name
        {
            $qual = $table_name.text;
            $params = new LinkedList<Serializable>();
        }
//    | correlation_name TODO
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

where_clause returns [String sql, List<Serializable> params]:
      ^(WHERE search_condition)
        {
            select_where = $search_condition.sql;
            select_where_params = $search_condition.params;
        }
    | /* nothing */
        {
            select_where = null;
        }
    ;

search_condition returns [String sql, List<Serializable> params]:
    b1=boolean_term { $sql = $b1.sql; $params = $b1.params; }
    (OR b2=boolean_term { $sql += " OR " + $b2.sql; $params.addAll($b2.params); } )*
    ;

boolean_term returns [String sql, List<Serializable> params]:
    b1=boolean_factor { $sql = $b1.sql; $params = $b1.params; }
    (AND b2=boolean_factor { $sql += " AND " + $b2.sql; $params.addAll($b2.params); } )*
    ;

boolean_factor returns [String sql, List<Serializable> params]:
      b=boolean_test { $sql = $b.sql; $params = $b.params; }
    | NOT b=boolean_test { $sql = "NOT " + $b.sql; $params = $b.params; }
    ;

boolean_test returns [String sql, List<Serializable> params]:
      p=predicate { $sql = $p.sql; $params = $p.params; }
    | LPAR s=search_condition RPAR { $sql = "(" + $s.sql + ")"; $params = $s.params; }
    ;

predicate returns [String sql, List<Serializable> params]
@init {
    List<Object> literals = new ArrayList<Object>();
}:
      ^(UN_OP un_op un_arg)
        {
            String op;
            int token = $un_op.start.getType();
            switch (token) {
                case IS_NULL:
                    op = " IS NULL";
                    break;
                case IS_NOT_NULL:
                    op = " IS NOT NULL";
                    break;
                default:
                    throw new UnwantedTokenException(token, input);
            }
            $sql = $un_arg.sql + op;
            $params = $un_arg.params;
        }
    | ^(BIN_OP bin_op arg1=bin_arg arg2=bin_arg)
        {
            String op;
            String arg2sql = $arg2.sql;
            int token = $bin_op.start.getType();
            switch (token) {
                case EQ:
                    op = "=";
                    break;
                case NEQ:
                    op = "<>";
                    break;
                case LT:
                    op = "<";
                    break;
                case LTEQ:
                    op = "<=";
                    break;
                case GT:
                    op = ">";
                    break;
                case GTEQ:
                    op = ">=";
                    break;
                case LIKE:
                    op = "LIKE";
                    break;
                case NOT_LIKE:
                    op = "NOT LIKE";
                    break;
                case IN:
                    op = "IN";
                    arg2sql = '(' + arg2sql + ')';
                    break;
                case NOT_IN:
                    op = "NOT IN";
                    arg2sql = '(' + arg2sql + ')';
                    break;
                default:
                    throw new UnwantedTokenException(token, input);
            }
            $sql = "(" + $arg1.sql + " " + op + " " + arg2sql + ")";
            $params = $arg1.params;
            $params.addAll($arg2.params);
        }
    | ^(BIN_OP_ANY bin_op_any bin_arg mvc=multi_valued_column_reference)
        {
            String op;
            int token = $bin_op_any.start.getType();
            boolean neg;
            switch (token) {
                case EQ:
                    op = "=";
                    neg = false;
                    break;
                case NEQ:
                    op = "=";
                    neg = true;
                    break;
                case IN:
                    op = "IN";
                    neg = false;
                    break;
                case NOT_IN:
                    op = "IN";
                    neg = true;
                    break;
                default:
                    throw new UnwantedTokenException(token, input);
            }
            Column col = $mvc.col;
            String qual = $mvc.qual;
            String mqual = $mvc.mqual;
            String realTableName = col.getTable().getRealTable().getQuotedName();
            String tableAlias = col.getTable().getQuotedName();
            Table hierTable = queryMaker.getTable(queryMaker.hierTable, qual);
            String hierMainId = hierTable.getColumn(Model.MAIN_KEY).getFullQuotedName();
            String multiMainId = col.getTable().getColumn(Model.MAIN_KEY).getFullQuotedName();
            $sql = String.format("\%sEXISTS (SELECT 1 FROM \%s \%s WHERE"
                + " \%s = \%s AND \%s \%s \%s)",
                neg ? "NOT " : "",
                realTableName, tableAlias,
                hierMainId, multiMainId,
                col.getFullQuotedName(), op, $bin_arg.sql);
            $params = $bin_arg.params;
        }
    | ^(FUNC func_name (literal { literals.add($literal.value); })*)
        {
            String qual;
            String arg;
            if (literals.size() == 2) {
                qual = (String) literals.get(0);
                arg = (String) literals.get(1);
            } else {
                qual = null;
                arg = (String) literals.get(0);
            }
            List<Serializable> params = new LinkedList<Serializable>();
            int func = $func_name.start.getType();
            switch (func) {
                case IN_FOLDER:
                    $sql = queryMaker.getInFolderSql(qual, arg, params);
                    break;
                case IN_TREE:
                    $sql = queryMaker.getInTreeSql(qual, arg, params);
                    break;
                case CONTAINS:
                    $sql = queryMaker.getContainsSql(qual, arg, params);
                    break;
                case ID:
                default:
                    throw new UnwantedTokenException(func, input);
            }
            $params = params;
        }
    ;

un_op:
    IS_NULL | IS_NOT_NULL;

un_arg returns [String sql, List<Serializable> params]:
    c=column_reference { $sql = $c.sql; $params = $c.params; }
    ;

bin_op:
    EQ | NEQ | LT | GT | LTEQ | GTEQ | LIKE | NOT_LIKE | IN | NOT_IN ;

bin_op_any:
    EQ | NEQ | IN | NOT_IN;

bin_arg returns [String sql, List<Serializable> params]:
      v=value_expression { $sql = $v.sql; $params = $v.params; }
    | l=literal
        {
          $sql = "?";
          $params = new LinkedList<Serializable>(Collections.singleton($l.value));
        }
    | ^(LIST
         l1=literal
           {
             $sql = "?";
             $params = new LinkedList<Serializable>(Collections.singleton($l1.value));
           }
         (l2=literal
           {
             $sql += ", ?";
             $params.add($l2.value);
           }
         )*
       )
    ;

func_name:
    IN_FOLDER | IN_TREE | CONTAINS | ID;

literal returns [Serializable value]:
      NUM_LIT
        {
            try {
                $value = Long.valueOf($NUM_LIT.text);
            } catch (NumberFormatException e) {
                $value = Double.valueOf($NUM_LIT.text);
            }
        }
    | STRING_LIT
        {
            String s = $STRING_LIT.text;
            $value = s.substring(1, s.length() - 1);
        }
    | TIME_LIT
        {
            String s = $TIME_LIT.text;
            s = s.substring(s.indexOf('\'') + 1, s.length() - 1);
            try {
                $value = GregorianCalendar.fromAtomPub(s);
            } catch (IllegalArgumentException e) {
                throw new UnwantedTokenException(Token.INVALID_TOKEN_TYPE, input);
            }
        }
    | BOOL_LIT
        {
            $value = Boolean.valueOf($BOOL_LIT.text);
        }
    ;

value_expression returns [String sql, List<Serializable> params]:
      c=column_reference { $sql = $c.sql; $params = $c.params; }
//    | numeric_value_function TODO
    ;

column_reference returns [String sql, List<Serializable> params]:
    ^(COL qualifier? column_name)
      {
          String c = $column_name.start.getText();
          String qual = $qualifier.qual;
          $sql = queryMaker.referToColumnInWhere(c, qual);
          $params = new LinkedList<Serializable>();
      }
    ;

multi_valued_column_reference returns [Column col, String qual, String mqual]:
    ^(COL qualifier? column_name)
      {
          String c = $column_name.start.getText();
          String mqual = "nxm" + multiref++;
          $col = queryMaker.findMultiColumn(c, mqual);
          $qual = $qualifier.qual; // qualifier in original query, for join with hier
          $mqual = mqual; // qualifier generated internally for subselect table
      }
    ;

order_by_column_reference returns [String sql, List<Serializable> params]:
    ^(COL qualifier? column_name)
      {
          String c = $column_name.start.getText();
          String qual = $qualifier.qual;
          $sql = queryMaker.referToColumnInOrderBy(c, qual);
          $params = new LinkedList<Serializable>();
      }
    ;

order_by_clause:
    ^(ORDER_BY sort_specification+)
    ;

sort_specification:
    c=order_by_column_reference ( o=ASC | o=DESC )
      {
          String col = $c.sql;
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
