function deleteItem(id, elementId, popupId) {
  if (confirm('Delete this item? It will be gone forever.')) {
    document.getElementById(elementId).style.display = 'none';
    document.getElementById(popupId).style.display = 'none';
    $.ajax({ url: '/i/' + id, type: 'DELETE' });
  }
  return false;
}

function showItemExtras(id) {
  document.getElementById(id).style.display = 'block';
  return false;
}

// flag to set if item-extras div should be hidden
var cancel = false;

// hides item-extras div if cancel flag is not set
function hideItemExtras(element) {
  if (!cancel) {
    element.style.display = 'none';
  }
  cancel = false;
  return false;
}

// sets the cancel flag when called
function cancelHideItemExtras(element) {
  cancel = true;
  return false;
}