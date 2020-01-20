const rc = routes.eu.seitzal.opentab.controllers.RESTController;

function parseLangStatus(langstatus) {
  switch(langstatus) {
    case 1:
      return '<td class="text-body table-primary">EPL</td>';
    case 2:
      return '<td class="text-body table-info">ESL</td>';
    case 3:
      return '<td class="text-body table-success">EFL</td>';
    default:
      return '<td class="text-body table-secondary">Unknown</td>';
  }
}

const ratingGradient = [
  "#dc3545",
  "#d36233",
  "#cf7e32",
  "#ca9931",
  "#c6b330",
  "#b8c12e",
  "#98bd2d",
  "#79b92c",
  "#5cb42b",
  "#29ab2e",
  "#28a745"]

function parseRating(rating) {
  return '<td class="text-body" style="background-color: ' + ratingGradient[rating] + '">' + rating + '</td>';
}

function url_get(key) {
  let url = "" +  window.location;
  let urlsplit = url.split('?');
  if (urlsplit.length == 2) {
    let entries = urlsplit[1].split('&');
    for (i = 0; i < entries.length; i++) {
      var tuple = entries[i].split('=');
      if (decodeURI(tuple[0]) == key) {
        return decodeURI(tuple[1]);
      }
    }
    return undefined;
  } else {
    return undefined;
  }
}
