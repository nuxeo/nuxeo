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

package org.nuxeo.ecm.core.storage.sql.db.dialect;

import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * MySQL-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectMySQL extends Dialect {

    public DialectMySQL(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(new MySQL5InnoDBDialect(), metadata);
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        return String.format("CREATE FULLTEXT INDEX %s ON %s (%s)", indexName,
                tableName, StringUtils.join(columnNames, ", "));
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String whereExpr = "MATCH (`fulltext`.`simpletext`, `fulltext`.`binarytext`) AGAINST (?)";
        return new String[] { null, null, whereExpr, fulltextQuery };
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 2;
    }

    @Override
    public String getTableTypeString(Table table) {
        if (table.hasFulltextIndex()) {
            return " ENGINE=MyISAM";
        } else {
            return " ENGINE=InnoDB";
        }
    }

    @Override
    public boolean supportsUpdateFrom() {
        return true;
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        return true;
    }

    @Override
    public boolean needsOrderByKeysAfterDistinct() {
        return false;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?)", idColumnName);
    }

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType;
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "varchar(36)";
            break;
        case DB_IDENTITY:
            idType = "integer";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.TRUE, // always drop
                null, //
                "DROP FUNCTION IF EXISTS NX_IN_TREE", //
                String.format(
                        "CREATE FUNCTION NX_IN_TREE(id %s, baseid %<s) " //
                                + "RETURNS BOOLEAN " //
                                + "LANGUAGE SQL " //
                                + "READS SQL DATA " //
                                + "BEGIN" //
                                + "  DECLARE curid %<s DEFAULT id;" //
                                + "  IF baseid IS NULL OR id IS NULL OR baseid = id THEN" //
                                + "    RETURN FALSE;" //
                                + "  END IF;" //
                                + "  LOOP" //
                                + "    SELECT parentid INTO curid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF curid IS NULL THEN" //
                                + "      RETURN FALSE; " //
                                + "    ELSEIF curid = baseid THEN" //
                                + "      RETURN TRUE;" //
                                + "    END IF;" //
                                + "  END LOOP;" //
                                + "END" //
                        , idType)));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.TRUE, // always drop
                null, //
                "DROP FUNCTION IF EXISTS NX_ACCESS_ALLOWED", //
                String.format(
                        "CREATE FUNCTION NX_ACCESS_ALLOWED" //
                                + "(id %s, users VARCHAR(10000), perms VARCHAR(10000)) " //
                                + "RETURNS BOOLEAN " //
                                + "BEGIN" //
                                + "  DECLARE allusers VARCHAR(10000) DEFAULT CONCAT('|',users,'|');" //
                                + "  DECLARE allperms VARCHAR(10000) DEFAULT CONCAT('|',perms,'|');" //
                                + "  DECLARE first BOOLEAN DEFAULT TRUE;" //
                                + "  DECLARE curid %<s DEFAULT id;" //
                                + "  DECLARE newid %<s;" //
                                + "  DECLARE gr BIT;" //
                                + "  DECLARE pe VARCHAR(1000);" //
                                + "  DECLARE us VARCHAR(1000);" //
                                + "  WHILE curid IS NOT NULL DO" //
                                + "    BEGIN" //
                                + "      DECLARE done BOOLEAN DEFAULT FALSE;" //
                                + "      DECLARE cur CURSOR FOR" //
                                + "        SELECT `grant`, `permission`, `user` FROM `acls`" //
                                + "        WHERE `acls`.`id` = curid ORDER BY `pos`;" //
                                + "      DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;" //
                                + "      OPEN cur;" //
                                + "      REPEAT " //
                                + "        FETCH cur INTO gr, pe, us;" //
                                + "        IF NOT done THEN" //
                                + "          IF LOCATE(CONCAT('|',us,'|'), allusers) <> 0 AND LOCATE(CONCAT('|',pe,'|'), allperms) <> 0 THEN" //
                                + "            CLOSE cur;" //
                                + "            RETURN gr;" //
                                + "          END IF;" //
                                + "        END IF;" //
                                + "      UNTIL done END REPEAT;" //
                                + "      CLOSE cur;" //
                                + "    END;" //
                                + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF first AND newid IS NULL THEN" //
                                + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;" //
                                + "    END IF;" //
                                + "    SET first = FALSE;" //
                                + "    SET curid = newid;" //
                                + "  END WHILE;" //
                                + "  RETURN FALSE; " //
                                + "END" //
                        , idType)));
        return statements;
    }

    protected static class DebugStatements {
        public ConditionalStatement makeDebugTable() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP TABLE IF EXISTS NX_DEBUG_TABLE", //
                    "CREATE TABLE NX_DEBUG_TABLE (id INTEGER AUTO_INCREMENT PRIMARY KEY, log VARCHAR(10000))");
        }

        public ConditionalStatement makeNxDebug() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP PROCEDURE IF EXISTS NX_DEBUG", //
                    String.format("CREATE PROCEDURE NX_DEBUG(line VARCHAR(10000)) " //
                            + "LANGUAGE SQL " //
                            + "BEGIN " //
                            + "  INSERT INTO NX_DEBUG_TABLE (log) values (line);" //
                            + "END" //
                    ));
        }
    }

}
