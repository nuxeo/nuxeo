function placePopup(docid)
{
	if (docid=="")
		return;
	doc=document.getElementById('docRef:'+docid);
	menu=document.getElementById("contextMenu");
	if (!menu)
	{
		alert("CtxMnu not found");
		return;
	}
	menu.style.position="absolute";
	menu.style.left=doc.offsetLeft+10+'px';
	menu.style.top=doc.offsetTop+10+'px';
	menu.style.visibility="visible";
	menu.style.zIndex="1000";

	window.onclick=hideContextMenu;
	//document.body.addEventListener("click","hideContextMenu",true);
}

function hideContextMenu(evt)
{
	//alert("Hide CtxMnu");
	menu=document.getElementById("contextMenu");
	menu.style.visibility="hidden";
}
