import org.nuxeo.theme.themes.ThemeManager
import org.nuxeo.theme.themes.ThemeIOException

src = Request.getParameter("src")
indent = Request.getParameter("indent")

res = 1
try {
    ThemeManager.saveTheme(src, new Integer(indent))
} catch (ThemeIOException e) {
    res = 0
}

Response.writer.write(res)

