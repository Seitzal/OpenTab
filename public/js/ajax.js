const rc = routes.opentab.controllers.RESTController;
const ac = routes.opentab.controllers.AuthController;

function signIn(username, password, success, failure) {
  let rq = ac.signIn()
  rq.headers = {"Content-Type": "application/json"}
  rq.data = JSON.stringify({username: username, password: password});
  rq.success = (data) => {
    store.commit("setApiKey", data[0])
    Cookies.set("api_key", data[0])
    Cookies.set("api_key.exp", data[1].expires)
    Cookies.set("username", username)
    success()
  }
  rq.error = failure
  $.ajax(rq)
}

function signOut(then) {
  let rq = ac.signOut()
  rq.headers = {Authorization: store.state.api_key}
  rq.success = () => {
    store.commit("setApiKey", undefined)
    Cookies.remove("api_key")
    then()
  }
  rq.error = (xhr) => xhr.responseText
  $.ajax(rq)
}

function loadTabs(then = function(){}) {
  store.commit("setTabsUpToDate", false)
  let rq = rc.getAllTabs()
  rq.headers = store.getters.signedIn ? {Authorization: store.state.api_key} : {}
  rq.success = (data) => {
    store.commit("setTabs", data)
    store.commit("setTabsUpToDate", true)
    then()
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadPermissions(then = function(){}) {
  let rq = rc.getAllPermissions()
  rq.headers = store.getters.signedIn ? {Authorization: store.state.api_key} : {}
  rq.success = (data) => {
    store.commit("setPermissions", data)
    then()
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadTeams() {
  store.commit("setTeamsUpToDate", false)
  let rq = rc.getAllTeams(store.state.tabid)
  rq.headers = store.getters.signedIn ? {Authorization: store.state.api_key} : {}
  rq.success = (data) =>  {
    store.commit("setTeams", data)
    store.commit("setTeamsUpToDate", true)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function createTeam(team, then) {
  let rq = rc.createTeam()
  rq.headers = {Authorization: store.state.api_key}
  rq.data = {
    tabid: store.state.tabid,
    name: team.name,
    delegation: team.delegation != "" ? team.delegation : team.name,
    status: team.status
  }
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function updateTeam(team, then) {
  let rq = rc.updateTeam(team.id)
  rq.headers = {Authorization: store.state.api_key}
  rq.data = {
    name: team.name,
    delegation: team.delegation,
    status: team.status
  }
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function deleteTeam(team, then) {
  let rq = rc.deleteTeam(team.id)
  rq.headers = {Authorization: store.state.api_key}
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function toggleTeam(team, then) {
  let rq = rc.toggleTeam(team.id)
  rq.headers = {Authorization: store.state.api_key}
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadSpeakers(then) {
  store.commit("setSpeakersUpToDate", false)
  let rq = rc.getAllSpeakers(store.state.tabid)
  rq.headers = store.getters.signedIn ? {Authorization: store.state.api_key} : {}
  rq.success = (data) =>  {
    store.commit("setSpeakers", data)
    store.commit("setSpeakersUpToDate", true)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function createSpeaker(speaker, then) {
  let rq = rc.createSpeaker()
  rq.headers = {Authorization: store.state.api_key}
  rq.data = speaker
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function updateSpeaker(speaker, then) {
  let rq = rc.updateSpeaker(speaker.id)
  rq.headers = {Authorization: store.state.api_key}
  rq.data = speaker
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function deleteSpeaker(speaker, then) {
  let rq = rc.deleteSpeaker(speaker.id)
  rq.headers = {Authorization: store.state.api_key}
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function ajaxFailure(jqXHR, textStatus, errorThrown) {
  console.error("AJAX failure: " + jqXHR.responseText + " (" + jqXHR.status + ")")
  if (jqXHR.responseText == "Invalid API key") {
    alert("Credential rejected by server. Please sign in again.")
    app.signout()
  } else if (jqXHR.responseText == "API key has expired") {
    alert("Your session has expired. Please sign in again.")
    app.signout()
  } else {
    alert("An unexpected error occured. Check browser console for details.")
  }
}
