const rc = routes.opentab.controllers.RESTController;
const ac = routes.opentab.controllers.AuthController;

function signIn(username, password, success, failure) {
  let rq = ac.getToken(false);
  rq.headers = {Authorization: "Basic " + btoa(username + ":" + password)};
  rq.success = (data) => {
    const exp = JSON.parse(atob(data.split(".")[1])).exp * 1000;
    store.commit("setApiKey", data);
    store.commit("setExp", exp);
    Cookies.set("api_key", data);
    Cookies.set("api_key.exp", exp);
    Cookies.set("username", username);
    success();
  };
  rq.error = failure;
  $.ajax(rq);
}

function signOut(then) {
  store.commit("setApiKey", undefined);
  Cookies.remove("api_key");
  Cookies.remove("api_key.exp");
  then();
}

function loadTabs(then = function(){}) {
  if (tokenExpired()) return;
  store.commit("setTabsUpToDate", false)
  let rq = rc.getAllTabs()
  rq.headers = bearerAuth();
  rq.success = (data) => {
    store.commit("setTabs", data)
    store.commit("setTabsUpToDate", true)
    then()
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadPermissions(then = function(){}) {
  if (tokenExpired()) return;
  let rq = rc.getAllPermissions()
  rq.headers = bearerAuth();
  rq.success = (data) => {
    store.commit("setPermissions", data)
    then()
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadTeams() {
  if (tokenExpired()) return;
  store.commit("setTeamsUpToDate", false)
  let rq = rc.getAllTeams(store.state.tabid)
  rq.headers = bearerAuth();
  rq.success = (data) =>  {
    store.commit("setTeams", data)
    store.commit("setTeamsUpToDate", true)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function createTeam(team, then) {
  if (tokenExpired()) return;
  let rq = rc.createTeam()
  rq.headers = bearerAuth();
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
  if (tokenExpired()) return;
  let rq = rc.updateTeam(team.id)
  rq.headers = bearerAuth();
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
  if (tokenExpired()) return;
  let rq = rc.deleteTeam(team.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function toggleTeam(team, then) {
  if (tokenExpired()) return;
  let rq = rc.toggleTeam(team.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadSpeakers(then) {
  if (tokenExpired()) return;
  store.commit("setSpeakersUpToDate", false)
  let rq = rc.getAllSpeakers(store.state.tabid)
  rq.headers = bearerAuth();
  rq.success = (data) =>  {
    store.commit("setSpeakers", data)
    store.commit("setSpeakersUpToDate", true)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function createSpeaker(speaker, then) {
  if (tokenExpired()) return;
  let rq = rc.createSpeaker()
  rq.headers = bearerAuth();
  rq.data = speaker
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function updateSpeaker(speaker, then) {
  if (tokenExpired()) return;
  let rq = rc.updateSpeaker(speaker.id)
  rq.headers = bearerAuth();
  rq.data = speaker
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function deleteSpeaker(speaker, then) {
  if (tokenExpired()) return;
  let rq = rc.deleteSpeaker(speaker.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadDelegations(then) {
  if (tokenExpired()) return;
  let rq = rc.getAllDelegations(store.state.tabid)
  rq.headers = bearerAuth();
  rq.success = (data) =>  {
    store.commit("setDelegations", data)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadJudges(then) {
  if (tokenExpired()) return;
  store.commit("setJudgesUpToDate", false)
  let rq = rc.getAllJudges(store.state.tabid)
  rq.headers = bearerAuth();
  rq.success = (data) =>  {
    store.commit("setJudges", data)
    store.commit("setJudgesUpToDate", true)}
  rq.error = ajaxFailure
  $.ajax(rq)
}

function createJudge(judge, then) {
  if (tokenExpired()) return;
  let rq = rc.createJudge()
  rq.headers = bearerAuth();
  rq.data = {...judge, tabid: store.state.tabid}
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function updateJudge(judge, ratingOnly = false, then) {
  if (tokenExpired()) return;
  let rq = rc.updateJudge(judge.id)
  rq.headers = bearerAuth();
  rq.data = ratingOnly ? {rating: judge.rating} : judge
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function toggleJudge(judge, then) {
  if (tokenExpired()) return;
  let rq = rc.toggleJudge(judge.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function deleteJudge(judge, then) {
  if (tokenExpired()) return;
  let rq = rc.deleteJudge(judge.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function loadClashes(judge, then) {
  if (tokenExpired()) return;
  let rq = rc.getClashesForJudge(judge.id)
  rq.headers = bearerAuth();
  rq.success = (data) => {
    store.commit("setClashes", data)
    then(data)
  }
  rq.error = ajaxFailure
  $.ajax(rq)
}

function deleteClash(judgeid, teamid, then) {
  if (tokenExpired()) return;
  let rq = rc.unsetClash(judgeid, teamid)
  rq.headers = bearerAuth();
  rq.success = then
  rq.error = ajaxFailure
  $.ajax(rq)
}

function setClash(judgeid, teamid, level, then) {
  if (tokenExpired()) return;
  let rq = rc.setClash(judgeid, teamid, level)
  rq.headers = bearerAuth();
  rq.success = then
  rq.error = ajaxFailure
  $.ajax(rq)
}

function ajaxFailure(jqXHR, textStatus, errorThrown) {
  console.error("AJAX failure: " + jqXHR.responseText + " (" + jqXHR.status + ")")
  if (jqXHR.responseText == "Token has expired.") {
    alert("Your session has expired. Please sign in again.")
    app.signout()
  }
}
