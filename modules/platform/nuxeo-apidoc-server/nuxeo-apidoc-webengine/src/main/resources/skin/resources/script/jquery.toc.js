(function ($) {

  $.fn.toc = function (tocList) {
    $(tocList).empty();
    $("h2").each(function (index) {
      const name = `toc${index}`;
      const text = $(this).text();
      $(this).before(`<a name="${name}" class="anchor"></a>`);
      $(tocList).append(`<li><a href="#${name}">${text}</a></li>`);
    });
  }

})(jQuery);