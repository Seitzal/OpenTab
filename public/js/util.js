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