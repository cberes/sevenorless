function getFollowUrl(username, follow) {
  return '//localhost:3000/u/' + username + '/' + (follow ? 'follow' : 'unfollow');
}

function follow(element, username) {
  var followText = 'Follow';
  if (element.innerHTML === followText) {
    element.innerHTML = 'Unfollow';
    $.post(getFollowUrl(username, true));
  } else {
    element.innerHTML = followText;
    $.post(getFollowUrl(username, false));
  }
  return false;
}
