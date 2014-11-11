import org.nuxeo.theme.Manager
import org.nuxeo.theme.formats.styles.Style

uid = Request.getParameter("uid")
Style layer = (Style) Manager.getUidManager().getObjectByUid(uid);
if (layer != null) {
    Request.getSession(true).setAttribute("nxthemes.editor.current_style_layer", uid);
}
