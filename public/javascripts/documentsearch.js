$(function() {
  $(document).on("click", "input:text", function () {
    $(this).select();
  });
  $("#searchField").click();
});