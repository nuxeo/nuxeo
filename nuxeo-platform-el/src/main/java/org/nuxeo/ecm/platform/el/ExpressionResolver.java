package org.nuxeo.ecm.platform.el;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.MapELResolver;


public class ExpressionResolver extends CompositeELResolver {

    public ExpressionResolver() {
        super();
        add(new MapELResolver());
        add(new ArrayELResolver());
        add(new BeanELResolver());
    }
}
