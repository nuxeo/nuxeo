import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.styles.Style

id = Request.getParameter("id")
themeName = Request.getParameter("theme_name")
styleName = Request.getParameter("style_name")

ThemeManager themeManager = Manager.getThemeManager()

Style inheritedStyle = (Style) themeManager.getNamedObject(themeName, "style", styleName);
themeManager.deleteFormat(inheritedStyle);

themeManager.makeElementUseNamedStyle(id, null, themeName);
themeManager.removeNamedObject(themeName, "style", styleName);

