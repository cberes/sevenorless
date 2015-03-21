// see http://stackoverflow.com/questions/9334645/create-node-from-markup-string
function appendStringAsNodes(element, html) {
    var frag = document.createDocumentFragment(),
        tmp = document.createElement('body'), child;
    tmp.innerHTML = html;
    // Append elements in a loop to a DocumentFragment, so that the browser does
    // not re-render the document for each node
    while (child = tmp.firstChild) {
        frag.appendChild(child);
    }
    element.appendChild(frag); // Now, append all elements at once
    frag = tmp = null;
}

function toggleComments(id, elementId) {
  var element = document.getElementById(elementId);
  if (element.style.display == 'none') {
    element.style.display = 'block';
    var placeholders = element.getElementsByClassName('comments-placeholder');
    if (placeholders.length > 0) {
      // TODO: load comments
      var ph = placeholders.item(0);
      ph.parentNode.replaceChild(document.createTextNode("We did it!"), ph);
    }
  } else {
    element.style.display = 'none';
  }
}

function addComment(id, elementId, form) {
  // TODO: http://www.jstiles.com/Blog/How-To-Submit-a-Form-with-jQuery-and-AJAX
  return false;
}
