import $ from 'jquery';
import Cookies from 'js-cookie';

function ajaxFailure(jqXHR, textStatus, errorThrown) {
  console.error("AJAX failure: " + jqXHR.responseText + " (" + jqXHR.status + ")")
  if (jqXHR.responseText == "Token has expired.") {
    alert("Your session has expired. Please sign in again.")
    app.signout()
  }
}

export default {

  signIn: function(username, password, success, failure) {
    $.ajax({
      method: "GET",
      url: `${conf.apiPath}/token`,
      headers: {Authorization: "Basic " + btoa(username + ":" + password)},
      success: (data) => {
        const exp = JSON.parse(atob(data.split(".")[1])).exp * 1000;
        store.commit("setApiKey", data);
        store.commit("setExp", exp);
        Cookies.set("api_key", data);
        Cookies.set("api_key.exp", exp);
        Cookies.set("username", username);
        success();
      },
      error: failure
    });
  },

  signOut: function(then) {
    store.commit("setApiKey", undefined);
    Cookies.remove("api_key");
    Cookies.remove("api_key.exp");
    then();
  },

  loadTabs: function(then = function(){}) {
    if (tokenExpired()) return;
    store.commit("setTabsUpToDate", false)
    $.ajax({
      method: "GET",
      url: `${conf.apiPath}/tab`,
      headers: bearerAuth(),
      success: (data) => {
        store.commit("setTabs", data)
        store.commit("setTabsUpToDate", true)
        then()
      },
      error: ajaxFailure
    });
  },

  loadPermissions: function(then = function(){}) {
    if (tokenExpired()) return;
    $.ajax({
      method: "GET",
      url: `${conf.apiPath}/tab/permissions`,
      headers: bearerAuth(),
      success: (data) => {
        store.commit("setPermissions", data)
        then()
      },
      error: ajaxFailure
    });
  },

  loadTeams: function() {
    if (tokenExpired()) return;
    store.commit("setTeamsUpToDate", false)
    let rq = rc.getAllTeams(store.state.tabid)
    rq.headers = bearerAuth();
    rq.success = (data) =>  {
      store.commit("setTeams", data)
      store.commit("setTeamsUpToDate", true)}
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  createTeam: function(team, then) {
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
  },

  updateTeam: function(team, then) {
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
  },

  deleteTeam: function(team, then) {
    if (tokenExpired()) return;
    let rq = rc.deleteTeam(team.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  toggleTeam: function(team, then) {
    if (tokenExpired()) return;
    let rq = rc.toggleTeam(team.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  loadSpeakers: function(then) {
    if (tokenExpired()) return;
    store.commit("setSpeakersUpToDate", false)
    let rq = rc.getAllSpeakers(store.state.tabid)
    rq.headers = bearerAuth();
    rq.success = (data) =>  {
      store.commit("setSpeakers", data)
      store.commit("setSpeakersUpToDate", true)}
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  createSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    let rq = rc.createSpeaker()
    rq.headers = bearerAuth();
    rq.data = speaker
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  updateSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    let rq = rc.updateSpeaker(speaker.id)
    rq.headers = bearerAuth();
    rq.data = speaker
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  deleteSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    let rq = rc.deleteSpeaker(speaker.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  loadDelegations: function(then) {
    if (tokenExpired()) return;
    let rq = rc.getAllDelegations(store.state.tabid)
    rq.headers = bearerAuth();
    rq.success = (data) =>  {
      store.commit("setDelegations", data)}
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  loadJudges: function(then) {
    if (tokenExpired()) return;
    store.commit("setJudgesUpToDate", false)
    let rq = rc.getAllJudges(store.state.tabid)
    rq.headers = bearerAuth();
    rq.success = (data) =>  {
      store.commit("setJudges", data)
      store.commit("setJudgesUpToDate", true)}
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  createJudge: function(judge, then) {
    if (tokenExpired()) return;
    let rq = rc.createJudge()
    rq.headers = bearerAuth();
    rq.data = {...judge, tabid: store.state.tabid}
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  updateJudge: function(judge, ratingOnly = false, then) {
    if (tokenExpired()) return;
    let rq = rc.updateJudge(judge.id)
    rq.headers = bearerAuth();
    rq.data = ratingOnly ? {rating: judge.rating} : judge
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  toggleJudge: function(judge, then) {
    if (tokenExpired()) return;
    let rq = rc.toggleJudge(judge.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  deleteJudge: function(judge, then) {
    if (tokenExpired()) return;
    let rq = rc.deleteJudge(judge.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  loadClashes: function(judge, then) {
    if (tokenExpired()) return;
    let rq = rc.getClashesForJudge(judge.id)
    rq.headers = bearerAuth();
    rq.success = (data) => {
      store.commit("setClashes", data)
      then(data)
    }
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  deleteClash: function(judgeid, teamid, then) {
    if (tokenExpired()) return;
    let rq = rc.unsetClash(judgeid, teamid)
    rq.headers = bearerAuth();
    rq.success = then
    rq.error = ajaxFailure
    $.ajax(rq)
  },

  setClash: function(judgeid, teamid, level, then) {
    if (tokenExpired()) return;
    let rq = rc.setClash(judgeid, teamid, level)
    rq.headers = bearerAuth();
    rq.success = then
    rq.error = ajaxFailure
    $.ajax(rq)
  },

}
