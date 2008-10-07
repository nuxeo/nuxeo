import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.elements.ElementFormatter
import org.nuxeo.theme.events.EventContext
import org.nuxeo.theme.formats.styles.Style
import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.formats.FormatFactory
import org.nuxeo.theme.formats.FormatType
import org.nuxeo.theme.types.TypeFamily
import org.nuxeo.theme.formats.styles.Style

id = Request.getParameter("id")
themeName = Request.getParameter("theme_name")
styleName = Request.getParameter("style_name")

ThemeManager themeManager = Manager.getThemeManager()

if (themeManager.getNamedObject(themeName, "style", styleName) == null) {
    Style style = (Style) FormatFactory.create("style");
    style.setName(styleName);
    themeManager.setNamedObject(themeName, "style", style);
    themeManager.registerFormat(style);
}

themeManager.makeElementUseNamedStyle(id, styleName, themeName);

EventManager eventManager = Manager.getEventManager()
eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(element, null));
eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(element, null));
