function toggleComments(id, elementId) {
  var element = document.getElementById(elementId);
  if (element.style.display == 'none') {
    element.style.display = 'block';
    var placeholders = element.getElementsByClassName('loading');
    if (placeholders.length > 0) {
      var ph = placeholders.item(0).parentNode;
      $.get('/comments/' + id, function(data) {
        ph.innerHTML = data;
      });
    }
  } else {
    element.style.display = 'none';
  }
}

function addComment(id, elementId, form) {
  var element = document.getElementById(elementId);
  var placeholders = element.getElementsByClassName('comments-placeholder');
  var ph = placeholders.item(0);
  $.post('/comments/' + id, $(form).serialize()).done(function( data ) {
    ph.innerHTML = data;
  });
  form.reset();
  return false;
}
