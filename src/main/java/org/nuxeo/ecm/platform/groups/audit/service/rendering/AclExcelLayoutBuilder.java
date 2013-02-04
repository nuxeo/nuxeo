package org.nuxeo.ecm.platform.groups.audit.service.rendering;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

import com.google.common.collect.Multimap;

/**
 * An excel layout builder that uses one column per user, and write the list
 * of existing ACL in one cell, by using "," as separator character.
 *
 * A denying ACL is indicated by !S, where S is the short name given to the ACL,
 * as stated by the {@link AclNameShortner}.
 *
 * @author Martin Pernollet <mpernollet@nuxeo.com>
 */
public class AclExcelLayoutBuilder {
	protected static Logger log = Logger.getLogger(AclExcelLayoutBuilder.class);

	protected ExcelBuilder excel = new ExcelBuilder();
	protected static int CELL_WIDTH_UNIT = 256;

	/* layout */
	protected ReportLayout layout;
	protected Map<String, Integer> userColumn;
	protected Map<Pair<String, String>, Integer> userAclColumn;
	protected int treeLineCursor = 0;
	protected CellStyle userHeaderStyle;
	protected CellStyle aclHeaderStyle;


	// protected int treeLineCursorRowStart = 1;

	private static ReportLayout defaultLayout() {
		ReportLayout layout = new ReportLayout();
		layout.userHeaderHeight = 1000;
		layout.userHeaderRotation = 45;
		layout.fileTreeColumnWidth = 2; // in number of char
		layout.aclColumnWidth = 4;
		layout.defaultRowHeight = 100;
		layout.splitPaneX = 500;
		layout.splitPaneY = 1500;
		layout.freezePaneRowSplit = 1;
		layout.treeLineCursorRowStart = 1;

		return layout;
	}

	/* tools */
	protected AclExtractor acl = new AclExtractor();
	protected AclNameShortner shortner = new AclNameShortner();
	protected DocumentTreeAnalysis preprocessing = new DocumentTreeAnalysis();

	public AclExcelLayoutBuilder() {
		this(defaultLayout());
	}

	public AclExcelLayoutBuilder(ReportLayout layout) {
		this.layout = layout;
	}

	/**
	 * Analyze and render an ACL audit for the complete repository in
	 * unrestricted mode.
	 */
	public void renderAudit(CoreSession session) throws ClientException {
		renderAudit(session, session.getRootDocument(), true);
	}

	/**
	 * Analyze and render an ACL audit for the complete document tree in
	 * unrestricted mode.
	 */
	public void renderAudit(CoreSession session, final DocumentModel doc)
			throws ClientException {
		renderAudit(session, doc, true);
	}

	/** Analyze and render an ACL audit for the input document and its children. */
	public void renderAudit(CoreSession session, final DocumentModel doc,
			boolean unrestricted) throws ClientException {
		if (!unrestricted) {
			doRenderAudit(session, doc);
		} else {
			UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
					session) {
				@Override
				public void run() throws ClientException {
					doRenderAudit(session, doc);
				}
			};
			runner.runUnrestricted();
		}
	}

	/* EXCEL RENDERING */

	protected void doRenderAudit(CoreSession session, DocumentModel doc)
			throws ClientException {
		// prepare few informations
		preprocessing.analyze(session);
		int maxDepth = preprocessing.getDocumentTreeDepth();

		// do rendering
		renderInit();
		renderHeader(maxDepth, preprocessing.getUserAndGroups(),
				preprocessing.getPermissions());
		renderFileTreeAndAclMatrix(session, doc, maxDepth);
		formatFileTreeCellLayout(maxDepth); // layout tree

		renderFinal();
	}

	/** Initialize layout data model and prebuilded cell styles*/
	protected void renderInit() {
		userColumn = new HashMap<String, Integer>();
		userAclColumn = new HashMap<Pair<String, String>, Integer>();

		userHeaderStyle = excel.newCellStyle();
		userHeaderStyle.setFont(excel.getBoldFont());
		userHeaderStyle.setAlignment(CellStyle.ALIGN_CENTER);
		if(layout.userHeaderRotation!=0)
			userHeaderStyle.setRotation((short) layout.userHeaderRotation);

		aclHeaderStyle = excel.newCellStyle();
		aclHeaderStyle.setFont(excel.newFont(layout.aclHeaderFontSize));
		aclHeaderStyle.setAlignment(CellStyle.ALIGN_CENTER);
		if(layout.aclHeaderRotation!=0)
			aclHeaderStyle.setRotation((short) layout.aclHeaderRotation);
	}

	/** Perform various general tasks, such as setting the current sheet zoom. */
	public void renderFinal() {
		excel.getCurrentSheet().setZoom(2,4);
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

	protected void renderHeaderUsers(int tableStartColumn, Set<String> userOrGroups) {
		CellStyle headerStyle = excel.newCellStyle();
		headerStyle.setRotation((short) layout.userHeaderRotation);

		int column = tableStartColumn;
		for (String userOrGroup : userOrGroups) {
			excel.setCell(0, column, userOrGroup, headerStyle);
			setUserColumn(column, userOrGroup);
			column++;
		}
		excel.setRowHeight(0, layout.userHeaderHeight);
	}

	/* FILE TREE AND MATRIX CONTENT RENDERING */

	/** start visiting the document tree and filling the spreadsheet */
	protected void renderFileTreeAndAclMatrix(CoreSession session,
			DocumentModel doc, int maxDepth) throws ClientException {
		treeLineCursor = layout.treeLineCursorRowStart;
		renderFileTreeAndAclMatrix(session, doc, 0, maxDepth);
	}

	/**
	 * Visit the document tree recursively to:
	 * <ul>
	 * <li>draw the tree in excel cells
	 * <li>report permissions for each user in a matrix
	 * </ul>
	 */
	protected void renderFileTreeAndAclMatrix(CoreSession session,
			DocumentModel doc, int depth, int maxDepth) throws ClientException {
		doRenderFileTreeAndAclMatrix(session, doc, depth, maxDepth);

		// continue the work recursively
		DocumentModelList list = session.getChildren(doc.getRef());
		for (DocumentModel child : list) {
			renderFileTreeAndAclMatrix(session, child, depth + 1, maxDepth);
		}
	}

	protected void doRenderFileTreeAndAclMatrix(CoreSession session,
			DocumentModel doc, int depth, int maxDepth) throws ClientException {
		renderFilename(doc, depth);
		renderAcl(doc);
		treeLineCursor++;
	}

	public void renderFilename(DocumentModel doc, int depth)
			throws ClientException {
		if (layout.printFullPath) {
			excel.setCell(treeLineCursor, 0, doc.getPathAsString());
		} else {
			excel.setCell(treeLineCursor, depth, doc.getTitle());
			if (depth > 0 && acl.hasLockInheritanceACE(doc)) {
				excel.setCell(treeLineCursor, depth-1, "", excel.getColoredCellStyle(ByteColor.BLUE));
			}
		}
	}

	/** Set column of size of each file tree column, and apply a freeze
	 * pan to fix the tree columns and header rows. */
	protected void formatFileTreeCellLayout(int maxDepth) {
		for (int i = 0; i < maxDepth - 1; i++) {
			excel.setColumnWidth(i, (int)(layout.fileTreeColumnWidth
					* CELL_WIDTH_UNIT));
		}
		excel.setColumnWidthAuto(maxDepth - 1);
		excel.setFreezePane(maxDepth, layout.freezePaneRowSplit);
	}

	/* ACL CELLS RENDERING */

	/** Render a row with all ACL of a given input file.*/
	protected void renderAcl(DocumentModel doc) throws ClientException {
		Multimap<String, Pair<String, Boolean>> userAcls = acl
				.getAclByUser(doc);

		for (String user : userAcls.keySet()) {
			int column = getUserColumn(user);
			String info = formatAcl(userAcls.get(user));

			excel.setCell(treeLineCursor, column, info);
		}
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
		if (ace.getRight())
			return formatPermission(ace.getLeft());
		else
			return "!" + formatPermission(ace.getLeft());
	}

	protected String formatPermission(String permission) {
		return shortner.getShortName(permission);
	}

	/* ACL COLUMN LAYOUT */

	/** Set the user column */
	public void setUserColumn(int column, String userOrGroup) {
		userColumn.put(userOrGroup, column);
	}

	/** Return the user column */
	public int getUserColumn(String user) {
		return userColumn.get(user);
	}

	public void setUserAclColumn(int column, Pair<String, String> userAcl) {
		userAclColumn.put(userAcl, column);
	}

	/** Return the user column */
	public int getUserAclColumn(Pair<String, String> userAcl) {
		return userAclColumn.get(userAcl);
	}

	/* */

	public ExcelBuilder getExcel() {
		return excel;
	}
}
