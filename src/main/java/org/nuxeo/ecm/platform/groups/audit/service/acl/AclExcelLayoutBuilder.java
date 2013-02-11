package org.nuxeo.ecm.platform.groups.audit.service.acl;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.nuxeo.ecm.core.api.ClientException;
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
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilderMultiSheet;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsAllContent;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;

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
 * <li>Render ACL matrix by
 * </ul>
 * </ul>
 *
 * One can apply a {@link IContentFilter} to ignore some users/groups.
 *
 * This report builder uses one column per user, and write the list of existing
 * ACL in one cell, by using "," as separator character.
 *
 * A denying ACL is indicated by !S, where S is the short name given to the ACL,
 * as stated by the {@link AclNameShortner}.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclExcelLayoutBuilder implements IAclExcelLayoutBuilder {
    protected static Log log = LogFactory.getLog(AclExcelLayoutBuilder.class);

    protected IExcelBuilder excel = new ExcelBuilder();

    protected static int CELL_WIDTH_UNIT = 256;

    public static int STATUS_ROW = 0;

    public static int STATUS_COL = 0;

    /* layout */
    protected ReportLayoutSettings layoutSettings;

    protected ReportLayout layout;

    protected int treeLineCursor = 0;

    protected CellStyle userHeaderStyle;

    protected CellStyle aclHeaderStyle;

    protected CellStyle lockInheritanceStyle;

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

    public AclExcelLayoutBuilder(ReportLayoutSettings layout,
            IContentFilter filter) {
        this.layoutSettings = layout;

        if (SpanMode.NONE.equals(layout.spanMode))
            excel = new ExcelBuilder();
        else if (SpanMode.COLUMN_OVERFLOW_ON_NEXT_SHEETS.equals(layout.spanMode)) {
            excel = new ExcelBuilderMultiSheet();
            ((ExcelBuilderMultiSheet) excel).setMultiSheetColumns(true);
        } else
            throw new IllegalArgumentException("layout span mode unknown: "
                    + layout.spanMode);

        if (filter == null)
            this.filter = new AcceptsAllContent();
        else
            this.filter = filter;

        if (layoutSettings.pageSize > 0)
            this.data = new DataProcessorPaginated(this.filter,
                    layoutSettings.pageSize);
        else
            this.data = new DataProcessor(this.filter);

        this.shortner = new AclNameShortner();
        this.layout = new ReportLayout();
    }

    @Override
    public void renderAudit(CoreSession session) throws ClientException {
        renderAudit(session, session.getRootDocument(), true);
    }

    @Override
    public void renderAudit(CoreSession session, final DocumentModel doc)
            throws ClientException {
        renderAudit(session, doc, true);
    }

    @Override
    public void renderAudit(CoreSession session, final DocumentModel doc,
            boolean unrestricted) throws ClientException {
        if (!unrestricted) {
            analyzeAndRender(session, doc);
        } else {
            UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                    session) {
                @Override
                public void run() throws ClientException {
                    analyzeAndRender(session, doc);
                }
            };
            runner.runUnrestricted();
        }
    }

    protected void analyzeAndRender(CoreSession session, final DocumentModel doc)
            throws ClientException {
        log.debug("start processing data");
        data.analyze(session, doc);

        render(data);
    }

    /* EXCEL RENDERING */

    protected void render(IDataProcessor data) throws ClientException {
        int minDepth = data.getDocumentTreeMinDepth();
        int maxDepth = data.getDocumentTreeMaxDepth();
        int colStart = maxDepth + 1;

        renderInit();
        renderHeader(colStart, data.getUserAndGroups(), data.getPermissions());
        renderFileTreeAndAclMatrix(data.getAllDocuments(), minDepth);
        formatFileTreeCellLayout(maxDepth, minDepth, colStart);
        renderLegend(data.getStatus(), data.getInformation());
        renderFinal();
    }

    /** Initialize layout data model and pre-built cell styles */
    protected void renderInit() {
        layout.reset();

        userHeaderStyle = excel.newCellStyle();
        userHeaderStyle.setFont(excel.getBoldFont());
        userHeaderStyle.setAlignment(CellStyle.ALIGN_CENTER);
        if (layoutSettings.userHeaderRotation != 0)
            userHeaderStyle.setRotation((short) layoutSettings.userHeaderRotation);

        aclHeaderStyle = excel.newCellStyle();
        aclHeaderStyle.setFont(excel.newFont(layoutSettings.aclHeaderFontSize));
        aclHeaderStyle.setAlignment(CellStyle.ALIGN_CENTER);
        if (layoutSettings.aclHeaderRotation != 0)
            aclHeaderStyle.setRotation((short) layoutSettings.aclHeaderRotation);

        lockInheritanceStyle = excel.newColoredCellStyle(ByteColor.BLUE);
    }

    /** Perform various general tasks, such as setting the current sheet zoom. */
    protected void renderFinal() {
        for (Sheet s : excel.getAllSheets()) {
            s.setZoom(layoutSettings.zoomRatioNumerator,
                    layoutSettings.zoomRatioDenominator);
        }
    }

    /* HEADER RENDERING */

    /**
     * Write users and groups on the first row. Memorize the user (or group)
     * column which can later be retrieved with getColumn(user)
     */
    protected void renderHeader(int tableStartColumn, Set<String> userOrGroups,
            Set<String> permission) {
        renderHeaderUsers(tableStartColumn, userOrGroups);
    }

    protected void renderHeaderUsers(int tableStartColumn,
            Set<String> userOrGroups) {
        int column = tableStartColumn;
        for (String userOrGroup : userOrGroups) {
            excel.setCell(0, column, userOrGroup, userHeaderStyle);
            layout.setUserColumn(column, userOrGroup);
            column++;
        }
        excel.setRowHeight(0, layoutSettings.userHeaderHeight);
    }

    /* FILE TREE AND MATRIX CONTENT RENDERING */

    protected void renderFileTreeAndAclMatrix(
            Collection<DocumentSummary> analyses, int minDepth)
            throws ClientException {
        treeLineCursor = layoutSettings.treeLineCursorRowStart;

        for (DocumentSummary summary : analyses) {
            renderFilename(summary.getTitle(), summary.getDepth() - minDepth,
                    summary.isAclLockInheritance());
            renderAcl(summary.getUserAcls());
            treeLineCursor++;
        }
    }

    protected void renderFilename(String title, int depth,
            boolean lockInheritance) throws ClientException {
        // draw title
        excel.setCell(treeLineCursor, depth, title);

        // draw ace inheritance locker
        if (depth > 0 && lockInheritance) {
            excel.setCell(treeLineCursor, depth - 1, "", lockInheritanceStyle);
        }
    }

    /** Render a row with all ACL of a given input file. */
    protected void renderAcl(Multimap<String, Pair<String, Boolean>> userAcls)
            throws ClientException {
        for (String user : userAcls.keySet()) {
            int column = layout.getUserColumn(user);
            String info = formatAcl(userAcls.get(user));

            excel.setCell(treeLineCursor, column, info);
        }
    }

    protected void renderLegend(ProcessorStatus status, String message) {
        ((ExcelBuilderMultiSheet) excel).setMultiSheetColumns(false);

        int s = excel.newSheet(excel.getCurrentSheetId() + 1, "Legend");
        excel.setCurrentSheetId(s);

        int row = STATUS_ROW;
        int col = STATUS_COL;
        int off = renderLegendErrorMessage(row, col, status, message);
        off = renderLegendAcl(off + 1, 0);
    }

    protected int renderLegendErrorMessage(int row, int col,
            ProcessorStatus status, String message) {
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
     * Renders all ACE separated by a ,
     *
     * Each ACE name is formated using {@link formatAce(Pair<String, Boolean>
     * ace)}
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
     * Set column of size of each file tree column, and apply a freeze pan to
     * fix the tree columns and header rows.
     */
    protected void formatFileTreeCellLayout(int maxDepth, int minDepth,
            int colStart) {
        int realMax = maxDepth - minDepth;
        for (int i = 0; i < realMax; i++) {
            excel.setColumnWidth(
                    i,
                    (int) (layoutSettings.fileTreeColumnWidth * CELL_WIDTH_UNIT));
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
