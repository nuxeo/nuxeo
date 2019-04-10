var lastSelectedRow = null;
var selectedElement = null;
function selectrow(row)
{
     row.className = "selected";
     selectedElement = row;
     if (lastSelectedRow!=null && lastSelectedRow!=selectedElement) {
         lastSelectedRow.className = "";
     }
     lastSelectedRow=row;
}
function checkScroll()
{
    if (document.body.scrollHeight > document.body.offsetHeight ||document.body.scrollWidth > document.body.offsetWidth)
       document.body.scroll="yes";
}
