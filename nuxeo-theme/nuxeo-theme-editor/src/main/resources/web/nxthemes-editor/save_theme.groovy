
import org.nuxeo.theme.themes.ThemeManager

src = Request.getParameter("src")
indent = Request.getParameter("indent")

res = true
try {
    ThemeManager.saveTheme(src, indent)
} catch (ThemeIOException e) {
    res = false
}

Response.writer.write(res)

