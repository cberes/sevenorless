function toggleTinyMce(element, id) {
  if (element.checked) {
    tinymce.execCommand('mceRemoveEditor', false, id);
  } else {
    tinymce.execCommand('mceAddEditor', false, id);
  }
}
