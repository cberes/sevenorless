function getFollowUrl(username, action) {
  return '//localhost:3000/u/' + username + '/' + action;
}

function follow(element, username) {
  var followText = 'Follow';
  if (element.innerHTML === followText) {
    element.innerHTML = 'Unfollow';
    $.post(getFollowUrl(username, 'follow'));
  } else {
    element.innerHTML = followText;
    $.post(getFollowUrl(username, 'unfollow'));
  }
  return false;
}

function approve(element, username) {
  element.innerHTML = 'Approved';
  $.post(getFollowUrl(username, 'approve'));
  return false;
}

function deny(element, username) {
  element.innerHTML = 'Denied';
  $.post(getFollowUrl(username, 'deny'));
  return false;
}
