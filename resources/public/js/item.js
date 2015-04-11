function deleteItem(id, element) {
  if (confirm('Delete this item? It will be gone forever.')) {
    element.style.display = 'none';
    $.ajax({ url: '/i/' + id, type: 'DELETE' });
  }
  return false;
}