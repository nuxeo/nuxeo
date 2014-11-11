package org.nuxeo.ecm.directory.sql;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.sql.repository.Column;
import org.nuxeo.ecm.directory.sql.repository.FieldMapper;

@XObject(value = "staticFilter")
public class SQLStaticFilter implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @XNode("column")
    protected String column;

    @XNode("operator")
    protected String operator = "=";

    @XNode("value")
    protected String value;

    @XNode("type")
    protected String type="string";


    public String getType() {
        return type;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public Column getDirectoryColumn() throws DirectoryException {
        return new Column(column,FieldMapper.getSqlField(type),column);
    }
}
