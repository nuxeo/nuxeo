package org.nuxeo.ecm.webengine.tests.security;

import java.text.ParseException;

import junit.framework.TestCase;
import org.nuxeo.ecm.webengine.security.PostfixExpression;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.ecm.webengine.security.PermissionService;

public class TestPermissionService extends TestCase {

    PermissionService ptb;

    @Override
    public void setUp() {
        ptb = PermissionService.getInstance();
    }

    public void test1() throws ParseException {
        String expr = "a|b&c";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a|b&c", postfix.toString().trim());
        assertEquals("PERM[a|b&c]", root.toString());
    }

    public void test2() throws ParseException {
        String expr = "a&b|c";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a&b|c", postfix.toString().trim());
        assertEquals("PERM[a&b|c]", root.toString());
    }

    public void test3() throws ParseException {
        String expr = "(a|b)&(c|d)";
        PostfixExpression postfix = new PostfixExpression(expr);
        Guard root = (Guard) postfix.visit(ptb);
        assertEquals("a|b c|d", postfix.toString().trim());
        assertEquals("PERM[c|d]", root.toString());
    }

}
