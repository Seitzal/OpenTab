import Cookies from 'js-cookie'

window.retrieveSession = function() {
  const exp = Cookies.get("api_key.exp")
  if (exp != undefined && timestamp() < exp) {
    return Cookies.get("api_key")
  } else {
    return undefined
  }
}

window.retrieveSessionExp = function() {
  const exp = Cookies.get("api_key.exp")
  if (exp != undefined && timestamp() < exp) {
    return Cookies.get("api_key.exp")
  } else {
    return undefined
  }
}

window.tokenExpired = function() {
  if(store.getters.signedIn && timestamp() > store.state.exp) {
    alert("Your session has expired. Please sign in again.");
    app.signout(app.$router.push("/"));
    return true;
  } else return false;
}

window.bearerAuth = function() {
  if (store.getters.signedIn) {
    return {"Authorization": "Bearer " + store.state.api_key}
  } else {
    return {}
  }
}

window.timestamp = function() {
  return (new Date()).getTime()
}

window.ratingColors = [
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
  "#28a745"];
