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
      url: `${conf.apiPath}/tabs`,
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
      url: `${conf.apiPath}/tabs/permissions`,
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
    $.ajax({
      method: "GET",
      url: `${conf.apiPath}/tab/${store.state.tabid}/teams`,
      headers: bearerAuth(),
      success: (data) =>  {
        store.commit("setTeams", data)
        store.commit("setTeamsUpToDate", true)
      },
      error: ajaxFailure
    });
  },

  createTeam: function(team, then) {
    if (tokenExpired()) return;
    if (team.delegation == "")
      team.delegation = team.name;
    $.ajax({
      method: "POST",
      url: `${conf.apiPath}/tab/${store.state.tabid}/team`,
      headers: bearerAuth(),
      data: JSON.stringify(team),
      success: then,
      error: ajaxFailure
    });
  },

  updateTeam: function(team, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "PATCH",
      url: `${conf.apiPath}/team/${team.id}`,
      headers: bearerAuth(),
      data: JSON.stringify(team),
      success: then,
      error: ajaxFailure
    })
  },

  deleteTeam: function(team, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "DELETE",
      url: `${conf.apiPath}/team/${team.id}`,
      headers: bearerAuth(),
      success: then,
      error: ajaxFailure
    });
  },

  toggleTeam: function(team, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "PATCH",
      url: `${conf.apiPath}/team/${team.id}`,
      headers: bearerAuth(),
      data: JSON.stringify({isActive: !team.isActive}),
      success: then,
      error: ajaxFailure
    })
  },

  loadSpeakers: function(then) {
    if (tokenExpired()) return;
    store.commit("setSpeakersUpToDate", false)
    $.ajax({
      method: "GET",
      url: `${conf.apiPath}/tab/${store.state.tabid}/speakers`,
      headers: bearerAuth(),
      success: (data) => {
        store.commit("setSpeakers", data)
        store.commit("setSpeakersUpToDate", true)
        // if (then != undefined) then(data)
      },
      error: ajaxFailure
    });
  },

  createSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "POST",
      url: `${conf.apiPath}/team/${speaker.teamId}/speaker`,
      headers: bearerAuth(),
      data: JSON.stringify(speaker),
      success: then,
      error: ajaxFailure
    });
  },

  updateSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "PATCH",
      url: `${conf.apiPath}/speaker/${speaker.id}`,
      headers: bearerAuth(),
      data: JSON.stringify(speaker),
      success: then,
      error: ajaxFailure
    });
  },

  deleteSpeaker: function(speaker, then) {
    if (tokenExpired()) return;
    $.ajax({
      method: "DELETE",
      url: `${conf.apiPath}/speaker/${speaker.id}`,
      headers: bearerAuth(),
      success: then,
      error: ajaxFailure
    });
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
