/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings.SpanMode;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ByteColor;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

import com.google.common.collect.Multimap;

/**
 * An excel layout builder that uses one group of columns per user, using one column for each right type (read, write,
 * etc).
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclExcelLayoutBuilderMultiColumn extends AclExcelLayoutBuilder {
    protected static int USERS_ROW = 0;

    protected static int PERMISSIONS_ROW = 1;

    public static ReportLayoutSettings defaultLayout() {
        ReportLayoutSettings layout = new ReportLayoutSettings();
        layout.userHeaderHeight = -1;
        layout.userHeaderRotation = 0;
        layout.aclHeaderHeight = -1;// fit vertically full ACL name 1800;
        layout.aclHeaderRotation = 0;

        layout.fileTreeColumnWidth = 2.5; // in number of char
        layout.aclColumnWidth = 2.5;
        layout.defaultRowHeight = 100;
        layout.splitPaneX = 500;
        layout.splitPaneY = 1500;
        layout.freezePaneRowSplit = 2;
        layout.treeLineCursorRowStart = 2;

        layout.aclHeaderCommentColSpan = 10;
        layout.aclHeaderCommentRowSpan = 2;
        layout.aclHeaderFontSize = 6; // in font unit

        layout.spanMode = SpanMode.COLUMN_OVERFLOW_ON_NEXT_SHEETS;

        layout.zoomRatioDenominator = 2;
        layout.zoomRatioNumerator = 1;

        layout.logoImageFile = "src/main/resources/file-delete.png";

        return layout;
    }

    /* Prebuilt styles */

    protected CellStyle acceptStyle;

    protected CellStyle acceptStyleLeft;

    protected CellStyle acceptStyleRight;

    protected CellStyle denyStyle;

    protected CellStyle denyStyleLeft;

    protected CellStyle denyStyleRight;

    protected CellStyle emptyStyle;

    protected CellStyle emptyStyleLeft;

    protected CellStyle emptyStyleRight;

    protected int logoPictureId = -1;

    public AclExcelLayoutBuilderMultiColumn() {
        super(defaultLayout());
    }

    public AclExcelLayoutBuilderMultiColumn(IContentFilter filter) {
        this(defaultLayout(), filter);
    }

    public AclExcelLayoutBuilderMultiColumn(ReportLayoutSettings layout, IContentFilter filter) {
        super(layout, filter);
    }

    @Override
    protected void renderInit() {
        super.renderInit();
        acceptStyle = excel.newColoredCellStyle(ByteColor.GREEN);

        acceptStyleLeft = excel.newColoredCellStyle(ByteColor.GREEN);
        acceptStyleLeft.setBorderLeft(BorderStyle.THIN);
        acceptStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        acceptStyleRight = excel.newColoredCellStyle(ByteColor.GREEN);
        acceptStyleRight.setBorderRight(BorderStyle.THIN);
        acceptStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        denyStyle = excel.newColoredCellStyle(ByteColor.RED);
        denyStyle.setFillPattern(FillPatternType.THIN_FORWARD_DIAG);  // TODO: Generalize other cells
        denyStyle.setFillBackgroundColor(excel.getColor(ByteColor.WHITE).getIndex());

        denyStyleLeft = excel.newColoredCellStyle(ByteColor.RED);
        denyStyleLeft.setBorderLeft(BorderStyle.THIN);
        denyStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        denyStyleRight = excel.newColoredCellStyle(ByteColor.RED);
        denyStyleRight.setBorderRight(BorderStyle.THIN);
        denyStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        emptyStyle = excel.newColoredCellStyle(ByteColor.WHITE);

        emptyStyleLeft = excel.newColoredCellStyle(ByteColor.WHITE);
        emptyStyleLeft.setBorderLeft(BorderStyle.THIN);
        emptyStyleLeft.setLeftBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        emptyStyleRight = excel.newColoredCellStyle(ByteColor.WHITE);
        emptyStyleRight.setBorderRight(BorderStyle.THIN);
        emptyStyleRight.setRightBorderColor(excel.getColor(ByteColor.BLACK).getIndex());

        if (layoutSettings.logoImageFile != null)
            try {
                logoPictureId = excel.loadPicture(layoutSettings.logoImageFile);
            } catch (IOException e) {
                log.error(e, e);
            }
    }

    /* HEADER RENDERING */

    /**
     * Write users and groups on the first row. Memorize the user (or group) column which can later be retrieved with
     * getColumn(user)
     */
    @Override
    protected void renderHeader(int tableStartColumn, Set<String> userOrGroups, Set<String> permissions) {
        renderHeaderPicture();
        renderHeaderUsers(tableStartColumn, userOrGroups, permissions);
        renderHeaderAcl(userOrGroups, permissions);
        formatHeaderRowHeight();
    }

    protected void renderHeaderPicture() {
        // excel.mergeRange(USERS_ROW, 0, PERMISSIONS_ROW, tableStartColumn-1);
        excel.setPicture(logoPictureId, 0, 0, false);
    }

    protected void renderHeaderUsers(int tableStartColumn, Set<String> userOrGroups, Set<String> permissions) {

        int userColumn = tableStartColumn;
        for (String user : userOrGroups) {
            // render the user column header
            excel.setCell(USERS_ROW, userColumn, user, userHeaderStyle);
            layout.setUserColumn(userColumn, user);

            // merge cells indicating user name
            final int from = userColumn;
            final int to = userColumn + permissions.size() - 1;
            if (from < ExcelBuilder.LAST_COLUMN && to < ExcelBuilder.LAST_COLUMN)
                excel.mergeRange(USERS_ROW, from, USERS_ROW, to);

            userColumn += permissions.size();
            log.debug("user header: " + USERS_ROW + "," + userColumn + " > " + user);
        }
    }

    protected void renderHeaderAcl(Set<String> userOrGroups, Set<String> permissions) {
        for (String user : userOrGroups) {
            // render ACL column header for this user
            int userColumn;
            int aclColumn = 0;
            int aclHeaderColumn = 0;
            String aclHeaderText;
            String aclHeaderShort;

            for (String permission : permissions) {
                userColumn = layout.getUserColumn(user);
                aclHeaderColumn = userColumn + aclColumn;
                aclHeaderText = permission;// formatPermission(permission);
                aclHeaderShort = formatPermission(permission);

                Cell cell = excel.setCell(PERMISSIONS_ROW, aclHeaderColumn, aclHeaderShort, aclHeaderStyle);
                excel.setColumnWidth(aclHeaderColumn, (int) (layoutSettings.aclColumnWidth * CELL_WIDTH_UNIT));

                // add a comment with the acl complete name
                if ((aclHeaderColumn + layoutSettings.aclHeaderCommentColSpan) < ExcelBuilder.LAST_COLUMN)
                    excel.addComment(cell, aclHeaderText, PERMISSIONS_ROW, aclHeaderColumn,
                            layoutSettings.aclHeaderCommentColSpan, layoutSettings.aclHeaderCommentRowSpan);

                layout.setUserAclColumn(aclHeaderColumn, Pair.of(user, permission));
                aclColumn++;

                log.debug("permission header: " + PERMISSIONS_ROW + "," + aclHeaderColumn + " > "
                        + formatPermission(permission));
            }
        }
    }

    protected void formatHeaderRowHeight() {
        if (layoutSettings.aclHeaderHeight != -1)
            excel.setRowHeight(PERMISSIONS_ROW, layoutSettings.aclHeaderHeight);
        if (layoutSettings.userHeaderHeight != -1)
            excel.setRowHeight(USERS_ROW, layoutSettings.userHeaderHeight);
    }

    /* FILE TREE AND MATRIX CONTENT RENDERING */

    @Override
    protected void renderAcl(Multimap<String, Pair<String, Boolean>> userAcls) {
        for (String user : userAcls.keySet()) {
            List<Pair<String, Boolean>> acls = new ArrayList<>(userAcls.get(user));
            acls.size();

            // TODO: IF ACLS not contain an ACL that should be first or last,
            // thus showing border, post draw white cells

            for (int i = 0; i < acls.size(); i++) {
                boolean isFirst = false;// (i == 0);
                boolean isLast = false;// (i == last);

                Pair<String, Boolean> ace = acls.get(i);
                String permission = ace.a;
                boolean accept = ace.b;
                int aclColumn = layout.getUserAclColumn(Pair.of(user, permission));
                String aceText = "";// formatAce(ace)

                if (accept) {
                    // draws an accept cell
                    renderAcceptCell(isFirst, isLast, aclColumn, aceText);
                } else {
                    // draws a deny cell
                    renderDenyCell(isFirst, isLast, aclColumn, aceText);
                }
            }
            // String info = formatAcl(userAcls.get(user));
        }
    }

    /**
     * Render a cell with a 'deny' color with left, right or no border according to its position.
     */
    protected void renderDenyCell(boolean isFirst, boolean isLast, int aclColumn, String aceText) {
        if (isFirst) {
            excel.setCell(treeLineCursor, aclColumn, aceText, denyStyleLeft);
        } else if (isLast) {
            excel.setCell(treeLineCursor, aclColumn, aceText, denyStyleRight);
        } else {
            excel.setCell(treeLineCursor, aclColumn, aceText, denyStyle);
        }
    }

    /**
     * Render a cell with a 'accept' color with left, right or no border according to its position.
     */
    protected void renderAcceptCell(boolean isFirst, boolean isLast, int aclColumn, String aceText) {
        if (isFirst) {
            excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyleLeft);
        } else if (isLast) {
            excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyleRight);
        } else {
            excel.setCell(treeLineCursor, aclColumn, aceText, acceptStyle);
        }
    }
}
