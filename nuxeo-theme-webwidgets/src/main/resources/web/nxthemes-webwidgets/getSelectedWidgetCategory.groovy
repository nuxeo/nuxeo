category = Request.getSession(true).getAttribute("nxthemes.webwidgets.widget_category")
if (category == null) {
    category = ""
}
return category
