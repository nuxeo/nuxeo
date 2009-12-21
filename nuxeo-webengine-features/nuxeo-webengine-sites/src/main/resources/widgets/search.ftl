<script type="text/javascript">
<!--

  function getCharCount(str,chr)
  {
    var count = 0;
    for(var i=0;i<str.length;i++)
    {
	if(str.charAt(i) == chr)
        {
          count ++;
        }
    }

    return count;
  }
  
  function verifyPostData()
  {
    var searchParam = document.getElementById('searchParam');
    if(searchParam)
    {
        if((searchParam.value.charAt(0) == '\\') && (searchParam.value.length < 2))
	{
		alert('Invalid string.');
	}
	else if((searchParam.value.charAt(0) == '*') || (searchParam.value.charAt(0) == '?'))
	{
		alert('Invalid string.');
	}
        else if(getCharCount(searchParam.value,"\"") % 2 != 0)
	{
		alert('Invalid string.');
	}
	else if(getCharCount(searchParam.value,"\'") % 2 != 0)
	{
		alert('Invalid string.');
	}
	else
	{
	   return true;
	}	
    }
    
    return false;
  }

//-->
</script>

<div class="searchService">
<form id="search" action="${This.path}/search"
  onsubmit="return verifyPostData();" method="POST"
  accept-charset="utf-8"><input class="directoryFilter"
  type="text" name="searchParam" id="searchParam" size="15" /> <input
  class="button" type="submit" name="search_page" value="Search"
  id="search_page" /></form>
</div>