function retrieveSession() {
  const exp = Cookies.get("api_key.exp")
  if (exp != undefined && timestamp() > exp) {
    return Cookies.get("api_key")
  } else {
    return undefined
  }
}

function timestamp() {
  return (new Date()).getTime()
}

const ratingColors = [
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