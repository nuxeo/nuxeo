import org.nuxeo.theme.Manager
import org.nuxeo.theme.editor.Events
import org.nuxeo.theme.events.EventContext

themeName = Request.getParameter("themeName")
theme = Manager.getThemeManager().getThemeByName(themeName)

res = true
if (theme == null) {
    res = false
}

if (res) {
    ThemeManager.repairTheme(theme)

    EventManager eventManager = Manager.getEventManager()
    eventManager.notify(Events.THEME_MODIFIED_EVENT, new EventContext(theme, null))
    eventManager.notify(Events.STYLES_MODIFIED_EVENT, new EventContext(theme, null))
}

Response.writer.write(res)
