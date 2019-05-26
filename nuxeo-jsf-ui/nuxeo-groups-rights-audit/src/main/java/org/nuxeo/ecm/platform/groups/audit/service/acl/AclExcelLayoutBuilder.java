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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings.SpanMode;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessorPaginated;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DocumentSummary;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.IDataProcessor;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.AclNameShortner;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ByteColor;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder.Type;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilderMultiSheet;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsAllContent;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.groups.audit.service.acl.utils.MessageAccessor;

import com.google.common.collect.Multimap;

/**
 * A builder works in three phases:
 * <ul>
 * <li>Fetch documents, possibly using paging.
 * <li>Extract a document summary for each document.
 * <li>Render documents' summary:
 * <ul>
 * <li>Render header and define column layout
 * <li>Render file tree and define row layout
 * <li>Render ACL matrix
 * </ul>
 * </ul>
 * One can apply a {@link IContentFilter} to ignore some users/groups. This report builder uses one column per user, and
 * write the list of existing ACL in one cell, by using "," as separator character. A denying ACL is indicated by !S,
 * where S is the short name given to the ACL, as stated by the {@link AclNameShortner}.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclExcelLayoutBuilder implements IAclExcelLayoutBuilder {
    protected static Log log = LogFactory.getLog(AclExcelLayoutBuilder.class);

    protected static final String PROPERTY_MAIN_SHEET_NAME = "message.acl.audit.xl.mainsheet";

    protected static final String PROPERTY_LEGEND_SHEET_NAME = "message.acl.audit.xl.legend";

    protected static final String PROPERTY_LEGEND_LOCK_INHERITANCE = "message.acl.audit.xl.legend.lockInheritance";

    protected static final String PROPERTY_LEGEND_PERM_DENIED = "message.acl.audit.xl.legend.denied";

    protected IExcelBuilder excel = new ExcelBuilder();

    protected static final int CELL_WIDTH_UNIT = 256;

    public static final int STATUS_ROW = 0;

    public static final int STATUS_COL = 0;

    /* layout */
    protected ReportLayoutSettings layoutSettings;

    protected ReportLayout layout;

    protected int treeLineCursor = 0;

    protected CellStyle userHeaderStyle;

    protected CellStyle aclHeaderStyle;

    protected CellStyle lockInheritanceStyle;

    protected CellStyle grayTextStyle;

    protected int mainSheetId;

    protected int legendSheetId;

    protected String mainSheetName;

    protected String legendSheetName;

    protected String legendLockInheritance = "Permission inheritance locked";

    protected String legendPermissionDenied = "Permission denied";

    public static ReportLayoutSettings defaultLayout() {
        ReportLayoutSettings layout = new ReportLayoutSettings();
        layout.userHeaderHeight = 1000;
        layout.userHeaderRotation = 45;
        layout.fileTreeColumnWidth = 2; // in number of char
        layout.aclColumnWidth = 4;
        layout.defaultRowHeight = 100;
        layout.splitPaneX = 500;
        layout.splitPaneY = 1500;
        layout.freezePaneRowSplit = 1;
        layout.treeLineCursorRowStart = 1;
        layout.spanMode = SpanMode.COLUMN_OVERFLOW_ON_NEXT_SHEETS;
        layout.zoomRatioDenominator = 2;
        layout.zoomRatioNumerator = 1;
        layout.showFullPath = false;

        // data fetch setting
        layout.pageSize = 1000;

        return layout;
    }

    /* tools */
    protected IContentFilter filter;

    protected AclNameShortner shortner;

    protected IDataProcessor data;

    public AclExcelLayoutBuilder() {
        this(defaultLayout());
    }

    public AclExcelLayoutBuilder(IContentFilter filter) {
        this(defaultLayout(), filter);
    }

    public AclExcelLayoutBuilder(ReportLayoutSettings layout) {
        this(layout, null);
    }

    public AclExcelLayoutBuilder(ReportLayoutSettings layout, IContentFilter filter) {
        this.layoutSettings = layout;

        if (SpanMode.NONE.equals(layout.spanMode))
            excel = new ExcelBuilder(Type.XLS, "Permissions"); // missing context, no I18N
        else if (SpanMode.COLUMN_OVERFLOW_ON_NEXT_SHEETS.equals(layout.spanMode)) {
            excel = new ExcelBuilderMultiSheet(Type.XLS, "Permissions"); // missing context, no I18N
            ((ExcelBuilderMultiSheet) excel).setMultiSheetColumns(true);
        } else
            throw new IllegalArgumentException("layout span mode unknown: " + layout.spanMode);

        if (filter == null)
            this.filter = new AcceptsAllContent();
        else
            this.filter = filter;

        if (layoutSettings.pageSize > 0)
            this.data = new DataProcessorPaginated(this.filter, layoutSettings.pageSize);
        else
            this.data = new DataProcessor(this.filter);

        this.shortner = new AclNameShortner();
        this.layout = new ReportLayout();
    }

    @Override
    public void renderAudit(CoreSession session) {
        renderAudit(session, session.getRootDocument(), true);
    }

    @Override
    public void renderAudit(CoreSession session, final DocumentModel doc) {
        renderAudit(session, doc, true);
    }

    @Override
    public void renderAudit(CoreSession session, final DocumentModel doc, boolean unrestricted) {
        renderAudit(session, doc, unrestricted, 0);
    }

    @Override
    public void renderAudit(CoreSession session, final DocumentModel doc, boolean unrestricted, final int timeout)
            {
        if (!unrestricted) {
            analyzeAndRender(session, doc, timeout);
        } else {
            UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(session) {
                @Override
                public void run() {
                    analyzeAndRender(session, doc, timeout);
                }
            };
            runner.runUnrestricted();
        }
    }

    protected void analyzeAndRender(CoreSession session, final DocumentModel doc, int timeout) {
        log.debug("start processing data");
        data.analyze(session, doc, timeout);

        configure(session);
        render(data);
    }

    /* EXCEL RENDERING */

    protected void configure(CoreSession session) {
        // mainSheetName = MessageAccessor.get(session, PROPERTY_MAIN_SHEET_NAME);
        legendSheetName = MessageAccessor.get(session, PROPERTY_LEGEND_SHEET_NAME);
        legendLockInheritance = MessageAccessor.get(session, PROPERTY_LEGEND_LOCK_INHERITANCE);
        legendPermissionDenied = MessageAccessor.get(session, PROPERTY_LEGEND_PERM_DENIED);
    }

    protected void render(IDataProcessor data) {
        int minDepth = data.getDocumentTreeMinDepth();
        int maxDepth = data.getDocumentTreeMaxDepth();
        int colStart = maxDepth + (layoutSettings.showFullPath ? 1 : 0);

        mainSheetId = excel.getCurrentSheetId();
        legendSheetId = excel.newSheet(excel.getCurrentSheetId() + 1, legendSheetName);

        renderInit();
        renderHeader(colStart, data.getUserAndGroups(), data.getPermissions());
        renderFileTreeAndAclMatrix(data.getAllDocuments(), minDepth, maxDepth);
        formatFileTreeCellLayout(maxDepth, minDepth, colStart);
        renderLegend(data.getStatus(), data.getInformation());
        renderFinal();
    }

    /** Initialize layout data model and pre-built cell styles */
    protected void renderInit() {
        layout.reset();

        userHeaderStyle = excel.newCellStyle();
        userHeaderStyle.setFont(excel.getBoldFont());
        userHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        if (layoutSettings.userHeaderRotation != 0)
            userHeaderStyle.setRotation((short) layoutSettings.userHeaderRotation);

        aclHeaderStyle = excel.newCellStyle();
        aclHeaderStyle.setFont(excel.newFont(layoutSettings.aclHeaderFontSize));
        aclHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        if (layoutSettings.aclHeaderRotation != 0)
            aclHeaderStyle.setRotation((short) layoutSettings.aclHeaderRotation);

        lockInheritanceStyle = excel.newColoredCellStyle(ByteColor.BLUE);

        grayTextStyle = excel.newCellStyle();
        Font f = excel.newFont();
        f.setColor(HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
        grayTextStyle.setFont(f);
        // grayTextStyle.set
    }

    /** Perform various general tasks, such as setting the current sheet zoom. */
    protected void renderFinal() {
        for (Sheet s : excel.getAllSheets()) {
            s.setZoom(layoutSettings.zoomRatioNumerator * layoutSettings.zoomRatioDenominator);
        }
    }

    /* HEADER RENDERING */

    /**
     * Write users and groups on the first row. Memorize the user (or group) column which can later be retrieved with
     * getColumn(user)
     */
    protected void renderHeader(int tableStartColumn, Set<String> userOrGroups, Set<String> permission) {
        renderHeaderUsers(tableStartColumn, userOrGroups);
    }

    protected void renderHeaderUsers(int tableStartColumn, Set<String> userOrGroups) {
        int column = tableStartColumn;
        for (String userOrGroup : userOrGroups) {
            excel.setCell(0, column, userOrGroup, userHeaderStyle);
            layout.setUserColumn(column, userOrGroup);
            column++;
        }
        excel.setRowHeight(0, layoutSettings.userHeaderHeight);
    }

    /* FILE TREE AND MATRIX CONTENT RENDERING */

    protected void renderFileTreeAndAclMatrix(Collection<DocumentSummary> analyses, int minDepth, int maxDepth)
            {
        treeLineCursor = layoutSettings.treeLineCursorRowStart;

        for (DocumentSummary summary : analyses) {
            renderFilename(summary.getTitle(), summary.getDepth() - minDepth, summary.isAclLockInheritance());

            if (layoutSettings.showFullPath)
                excel.setCell(treeLineCursor, maxDepth - minDepth + 1, summary.getPath());

            if (summary.getAclInheritedByUser() != null)
                renderAcl(summary.getAclByUser(), summary.getAclInheritedByUser());
            else
                renderAcl(summary.getAclByUser());
            treeLineCursor++;
        }
    }

    protected void renderFilename(String title, int depth, boolean lockInheritance) {
        // draw title
        excel.setCell(treeLineCursor, depth, title);

        // draw ace inheritance locker
        if (depth > 0 && lockInheritance) {
            excel.setCell(treeLineCursor, depth - 1, "", lockInheritanceStyle);
        }
    }

    /** Render a row with all ACL of a given input file. */
    protected void renderAcl(Multimap<String, Pair<String, Boolean>> userAcls) {
        renderAcl(userAcls, (CellStyle) null);
    }

    protected void renderAcl(Multimap<String, Pair<String, Boolean>> userAcls, CellStyle style) {
        for (String user : userAcls.keySet()) {
            int column = layout.getUserColumn(user);
            String info = formatAcl(userAcls.get(user));
            excel.setCell(treeLineCursor, column, info, style);
        }
    }

    /**
     * Render local AND inherited ACL.
     * <ul>
     * <li>Local acl only are rendered with default font.
     * <li>Inherited acl only are rendered with gray font.
     * <li>Mixed acl (local and inherited) are rendered with default font.
     * </ul>
     */
    protected void renderAcl(Multimap<String, Pair<String, Boolean>> localAcls,
            Multimap<String, Pair<String, Boolean>> inheritedAcls) {
        Set<String> users = new HashSet<>();
        users.addAll(localAcls.keySet());
        users.addAll(inheritedAcls.keySet());

        for (String user : users) {
            int column = layout.getUserColumn(user);
            String localAclsString = formatAcl(localAcls.get(user));
            String inheritedAclsString = formatAcl(inheritedAcls.get(user));

            if ("".equals(localAclsString) && "".equals(inheritedAclsString)) {
            } else if (!"".equals(localAclsString) && !"".equals(inheritedAclsString)) {
                String info = localAclsString + "," + inheritedAclsString;
                excel.setCell(treeLineCursor, column, info);
            } else if (!"".equals(localAclsString) && "".equals(inheritedAclsString)) {
                String info = localAclsString;
                excel.setCell(treeLineCursor, column, info);
            } else if ("".equals(localAclsString) && !"".equals(inheritedAclsString)) {
                String info = inheritedAclsString;
                excel.setCell(treeLineCursor, column, info, grayTextStyle);
            }
        }
    }

    protected void renderLegend(ProcessorStatus status, String message) {
        ((ExcelBuilderMultiSheet) excel).setMultiSheetColumns(false);

        excel.setCurrentSheetId(legendSheetId);

        int row = STATUS_ROW;
        int col = STATUS_COL;
        int off = renderLegendErrorMessage(row, col, status, message);
        off = renderLegendAcl(off + 1, 0);
        off++;
        excel.setCell(off, col, "", lockInheritanceStyle);
        excel.setCell(off, col + 1, legendLockInheritance);
        off++;
    }

    protected int renderLegendErrorMessage(int row, int col, ProcessorStatus status, String message) {
        if (!ProcessorStatus.SUCCESS.equals(status)) {
            excel.setCell(row++, col, "Status: " + status);
            if (message != null && !"".equals(message))
                excel.setCell(row++, col, "Message: " + message);
        }
        return row;
    }

    protected int renderLegendAcl(int row, int col) {
        excel.setCell(row++, col, "ACL meaning");
        for (String shortName : shortner.getShortNames()) {
            String fullName = shortner.getFullName(shortName);
            excel.setCell(row, col, shortName);
            excel.setCell(row, col + 1, fullName);
            row++;
        }
        return row;
    }

    /* ACL TEXT FORMATTER FOR MATRIX */

    /**
     * Renders all ACE separated by a , Each ACE name is formated using {@link formatAce(Pair<String, Boolean> ace)}
     *
     * @return
     */
    protected String formatAcl(Collection<Pair<String, Boolean>> acls) {
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (Pair<String, Boolean> ace : acls) {
            sb.append(formatAce(ace));
            if ((++k) < acls.size())
                sb.append(",");
        }
        return sb.toString();
    }

    protected String formatAce(Pair<String, Boolean> ace) {
        if (ace.b)
            return formatPermission(ace.a);
        else
            return "!" + formatPermission(ace.a);
    }

    protected String formatPermission(String permission) {
        return shortner.getShortName(permission);
    }

    /* CELL FORMATTER */

    /**
     * Set column of size of each file tree column, and apply a freeze pan to fix the tree columns and header rows.
     */
    protected void formatFileTreeCellLayout(int maxDepth, int minDepth, int colStart) {
        int realMax = maxDepth - minDepth;
        for (int i = 0; i < realMax; i++) {
            excel.setColumnWidth(i, (int) (layoutSettings.fileTreeColumnWidth * CELL_WIDTH_UNIT));
        }
        excel.setColumnWidthAuto(realMax);
        excel.setFreezePane(colStart, layoutSettings.freezePaneRowSplit);
    }

    /* */

    /** {@inheritDoc} */
    @Override
    public IExcelBuilder getExcel() {
        return excel;
    }
}
