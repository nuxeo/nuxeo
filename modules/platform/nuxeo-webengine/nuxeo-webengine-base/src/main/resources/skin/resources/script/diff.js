var dmp = new diff_match_patch();

function doCompare() {
  var text1 = $("#rev1").html();
  var text2 = $("#rev2").html();
  var ms_start = (new Date()).getTime();
  var d = dmp.diff_main(text1, text2);
  dmp.diff_cleanupSemantic(d);
  var ms_end = (new Date()).getTime();

  var ds = dmp.diff_prettyHtml(d);



  $('#diffoutput').append(ds);
  $('#difftime').append("Diff Time: " + (ms_end - ms_start) + "ms");
}
