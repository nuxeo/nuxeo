function constructToolTipC(elem){
    
   //constructing the tooltip, so this must be the new selected element!
   selectedElement=elem;

   var changes_html = elem.getAttribute("changes");
   var previous_id = elem.getAttribute("previous");
   var next_id = elem.getAttribute("next");
   var change_id = elem.getAttribute("changeId");
   return "<table class='diff-tooltip-link-changed'>"+
          "  <tr>"+
          "    <td class='diff-tooltip-prev'>"+
          "      <a class='diffpage-html-a' href=#"+previous_id+" onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-previous.gif' title='Différence précédente'/></a>"+
          "    </td>"+
          "    <td>"+
          "      &#160;<a href='#"+change_id+"'>"+translateDiffId(change_id)+"</a>&#160;"+
          "    </td>"+
          "    <td class='diff-tooltip-next'>"+
          "      <a class='diffpage-html-a' href='#"+next_id+"' onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-next.gif' title='Différence suivante'/></a>"+
          "    </td>"+
          "  </tr>"+
          "</table>";
}

function constructToolTipA(elem){
   
   //constructing the tooltip, so this must be the new selected element!
   selectedElement=elem;
   
   var previous_id = elem.getAttribute("previous");
   var next_id = elem.getAttribute("next");
   var change_id = elem.getAttribute("changeId");
   return "<table class='diff-tooltip-link'>"+
          "  <tr>"+
          "    <td class='diff-tooltip-prev'>"+
          "      <a class='diffpage-html-a' href=#"+previous_id+" onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-previous.gif' title='Différence précédente'/></a>"+
          "    </td>"+
          "    <td>"+
          "      &#160;<a href='#"+change_id+"'>"+translateDiffId(change_id)+"</a>&#160;"+
          "    </td>"+
          "    <td class='diff-tooltip-next'>"+
          "      <a class='diffpage-html-a' href='#"+next_id+"' onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-next.gif' title='Différence suivante'/></a>"+
          "    </td>"+
          "  </tr>"+
          "</table>";
}

function constructToolTipR(elem){
   
   //constructing the tooltip, so this must be the new selected element!
   selectedElement=elem;
   
   var previous_id = elem.getAttribute("previous");
   var next_id = elem.getAttribute("next");
   var change_id = elem.getAttribute("changeId");
   return "<table class='diff-tooltip-link'>"+
          "  <tr>"+
          "    <td class='diff-tooltip-prev'>"+
          "      <a class='diffpage-html-a' href=#"+previous_id+" onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-previous.gif' title='Différence précédente'/></a>"+
          "    </td>"+
          "    <td>"+
          "      &#160;<a href='#"+change_id+"'>"+translateDiffId(change_id)+"</a>&#160;"+
          "    </td>"+
          "    <td class='diff-tooltip-next'>"+
          "      <a class='diffpage-html-a' href='#"+next_id+"' onClick='scrollToEvent(event)'><img class='diff-icon' src='"+imagePath+"diff-next.gif' title='Différence suivante'/></a>"+
          "    </td>"+
          "  </tr>"+
          "</table>";
}

function translateDiffId(diffId) {
	var lastIndexOfDash = diffId.lastIndexOf("-"); 
	if (lastIndexOfDash > -1) {
		var diffType = diffId.substring(0, lastIndexOfDash);
		var diffCount = parseInt(diffId.substring(lastIndexOfDash + 1, diffId.length1)) + 1;
		if (diffType == "removed-diff") {
			return "Suppression n° " + diffCount; 
		} else if (diffType == "added-diff") {
			return "Ajout n° " + diffCount;
		} else if (diffType == "changed-diff") {
			return "Modification n° " + diffCount;
		}
	}
	return diffId;
}