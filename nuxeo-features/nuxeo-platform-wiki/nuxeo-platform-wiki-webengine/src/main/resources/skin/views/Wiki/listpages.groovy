import net.sf.json.JSONObject
import net.sf.json.JSONArray
import org.apache.commons.lang.StringUtils

writer = Response.writer

qfield = 'ecm:fulltext'
qtext = ''
qsort = 'dc:modified'
qorder = 'DESC'

if (Request.getParameter('qtype')) {
    qfield = Request.getParameter('qtype')
}
if (Request.getParameter('query')) {
    qtext = Request.getParameter('query')
}
if (Request.getParameter('sortname')) {
    qsort = Request.getParameter('sortname')
}
if (Request.getParameter('sortorder')) {
    qorder = StringUtils.upperCase(Request.getParameter('sortorder'))
}

if (qfield && qtext) {
    query = "SELECT * FROM Document WHERE  (${qfield} = '${qtext}') AND (ecm:path STARTSWITH '${Root.repositoryPath}') AND (ecm:isCheckedInVersion = 0) ORDER BY ${qsort}"
} else {
    query = "SELECT * FROM Document WHERE (ecm:isCheckedInVersion = 0)  AND (ecm:path STARTSWITH '${Root.repositoryPath}') ORDER BY ${qsort} ${qorder}"
}
//writer.write(query)
rdoc = Context.search(query)

rows = []
for (d in rdoc) {
    def c = [:]
    c['id'] = d.id
    c['cell'] = []
    c['cell'].add(d.getPropertyValue('dc:title').toString())
    c['cell'].add(d.getPropertyValue('dc:modified').time.toString())
    c['cell'].add(d.getPropertyValue('dc:creator').toString())
    //c['cell'].add(Context.getUrlPath(d))
    c['cell'].add(d.currentLifeCycleState.toString())
    rows.add(c)
}

dlist = [page: 1, total: 1, rows: rows]


jslist = JSONObject.fromObject(dlist)

writer.write(jslist.toString())
