import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style

uid = Request.getParameter("uid")
Style layer = (Style) ThemeManager.getFormatById(uid)
if (layer != null) {
    Request.getSession(true).setAttribute("nxthemes.editor.style_layer", uid)
}
