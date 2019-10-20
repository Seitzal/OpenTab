let draw = undefined;
let teams = undefined;
let pairings = undefined;
let emptyBox = undefined;

$("#navitem_rounds").addClass("text-light");
$("#box_pairings").ready(loadTeams);

$("#btn_save").click(saveAndExit);
$("#btn_lock").click(saveLockAndExit);
$("#btn_clear").click(() => {
  initTable();
  initTeams();
});
$("#btn_randompair").click(getDrawRandom);

function allowDrop(ev) {
  ev.preventDefault();
}

function drag(ev, el) {
  emptyBox = el.parentElement;
  ev.dataTransfer.setData("text", ev.target.id);

}

function dropUnassigned(ev, el) {
  ev.preventDefault();
  var data = ev.dataTransfer.getData("text");
  el.appendChild(document.getElementById(data));
  emptyBox = undefined;
}

function drop(ev, el) {
  ev.preventDefault();
  var data = ev.dataTransfer.getData("text");
  if (el.firstChild) {
    emptyBox.appendChild(el.firstChild);
  }
  el.appendChild(document.getElementById(data));
}

function loadDraw() {
  let req1 = rc.isDrawn(tabid, roundNumber);
  req1.headers = {"Authorization": api_key};
  req1.error = () => alert("Error checking draw status");
  req1.success = isDrawn => {
    if(isDrawn) {
      let req2 = rc.getDraw(tabid, roundNumber);
      req2.headers = {"Authorization": api_key};
      req2.error = () => alert("Error loading draw");
      req2.success = data => {
        draw = data;
        renderDraw();
      }
      $.ajax(req2);
    } else {
      initTable();
      initTeams();
    }
  };
  $.ajax(req1);
}

function loadTeams() {
  let req = rc.getAllTeams(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = data => {
    teams = data.filter(team => team.active); 
    loadDraw();
  };
  req.error = () => alert("Error loading teams");
  $.ajax(req);
}

function initTable() {
  let table = `
    <table id="table_pairings" class="table table-hover table-bordered m-0">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">Proposition</th>
          <th class="font-weight-normal">Opposition</th>
        </tr>
      </thead>
      <tbody>`;
  for (i = 0; i < teams.length; i += 2) {
    table += `
      <tr>
        <td class="td_pro" ondrop="drop(event, this)" ondragover="allowDrop(event)">&nbsp;</td>
        <td class="td_opp" ondrop="drop(event, this)" ondragover="allowDrop(event)">&nbsp;</td>
      </tr>`;
  }
  table += `
      </tbody>
    </table>`;
  $("#box_pairings").html(table);
}

function initTeams() {
  let tags = '';
  for (i = 0; i < teams.length; i++) {
    tags += '<span class="teamtag border bg-light rounded m-1 p-1"  draggable="true" ondragstart="drag(event, this)" ' +
    'id=tt_"' + teams[i].id + '" data-teamid="' + teams[i].id + '">' + teams[i].name + '</span>';
  }
  if (teams.length % 2 == 1) {
    tags += '<span id="byetag" class="border bg-warning rounded m-1 p-1" draggable="true" ondragstart="drag(event, this)">' +
    'BYE</span>';
  }
  $("#box_unassigned").html(tags);
}

function getDrawRandom() {
  let req = rc.getRandomPairings(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = data => {
    draw = data;
    renderDraw();
  };
  req.error = () => alert("Error loading draw");
  $.ajax(req);
}

function renderDraw() {
  $("#box_unassigned").empty();
  let table = `
    <table id="table_pairings" class="table table-hover table-bordered m-0">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">Proposition</th>
          <th class="font-weight-normal">Opposition</th>
        </tr>
      </thead>
      <tbody id="table_pairings_body">`;
  for (r = 0; r < draw.pairings.length; r++) {
    table +=
      '<tr>' +
      '<td class="td_pro" ondrop="drop(event, this)" ondragover="allowDrop(event)">' + 
      '<span class="teamtag border bg-light rounded m-1 p-1"  draggable="true" ondragstart="drag(event, this)" ' +
      'id=tt_"' + draw.pairings[r][0].id + '" data-teamid="' + draw.pairings[r][0].id + '">' + draw.pairings[r][0].name + '</span>' +
      '</td>' +
      '<td class="td_opp" ondrop="drop(event, this)" ondragover="allowDrop(event)">' + 
      '<span class="teamtag border bg-light rounded m-1 p-1"  draggable="true" ondragstart="drag(event, this)" ' +
      'id=tt_"' + draw.pairings[r][1].id + '" data-teamid="' + draw.pairings[r][1].id + '">' + draw.pairings[r][1].name + '</span>' +
      '</td>' +
      '</tr>';
  }
  if (draw.teamOnBye[0] != undefined) {
    table += 
      '<tr>' +
      '<td class="td_pro" ondrop="drop(event, this)" ondragover="allowDrop(event)">' + 
      '<span class="teamtag border bg-light rounded m-1 p-1"  draggable="true" ondragstart="drag(event, this)" ' +
      'id=tt_"' + draw.teamOnBye[0].id + '" data-teamid="' + draw.teamOnBye[0].id + '">' + draw.teamOnBye[0].name + '</span>' +
      '</td>' +
      '<td class="td_opp" ondrop="drop(event, this)" ondragover="allowDrop(event)">' + 
      '<span id="byetag" class="border bg-warning rounded m-1 p-1" draggable="true" ondragstart="drag(event, this)">BYE</span>' +
      '</td>' +
      '</tr>';
  }
  table += `
      </tbody>
    </table>`;
  $("#box_pairings").html(table);
}

function checkDraw() {
  if ($("#box_unassigned").children().length != 0) {
    return false;
  }
  let rows = $("#table_pairings_body").children();
  let newPairings = [];
  let newByeTeam = [];
  for(i = 0; i < rows.length; i++) {
    let row = rows[i];
    if (row.children[0].children[0].id == "byetag") {
      let byeTeamId = row.children[1].children[0].dataset["teamid"];
      newByeTeam.push(teams.filter(team => team.id == byeTeamId)[0]);
    } else if (row.children[1].children[0].id == "byetag") {
      let byeTeamId = row.children[0].children[0].dataset["teamid"];
      newByeTeam.push(teams.filter(team => team.id == byeTeamId)[0]);
    } else {
      let proTeamId = row.children[0].children[0].dataset["teamid"];
      let oppTeamId = row.children[1].children[0].dataset["teamid"];
      let proTeam = teams.filter(team => team.id == proTeamId)[0];
      let oppTeam = teams.filter(team => team.id == oppTeamId)[0];
      newPairings.push([proTeam, oppTeam]);
    }
  }
  draw = {pairings : newPairings, teamOnBye : newByeTeam};
  return true;
}

function saveAndExit() {
  if (checkDraw()) {
    let req = rc.setDraw(tabid, roundNumber)
    req.headers = {"Authorization": api_key, "Content-Type": "application/json"}
    req.data = JSON.stringify(draw);
    req.success = () => {
      window.location.href = encodeURI(app_location + "/tab/" + tabid + "/rounds");
    }
    req.error = () => alert("Error saving draw.");
    $.ajax(req);
  } else {
    alert("Please assign all teams before saving the draw.");
  }
}

function saveLockAndExit() {
  if (checkDraw()) {
    let req1 = rc.setDraw(tabid, roundNumber)
    req1.headers = {"Authorization": api_key, "Content-Type": "application/json"}
    req1.data = JSON.stringify(draw);
    req1.success = () => {
      let req2 = rc.lockRound(tabid, roundNumber)
      req2.headers = {"Authorization": api_key}
      req2.success = () => {
        window.location.href = encodeURI(app_location + "/tab/" + tabid + "/rounds");
      }
      req2.error = (xhr, ajaxOptions, thrownError) => {
        alert(xhr.responseText);
      }
      $.ajax(req2);
    }
    req1.error = () => alert("Error saving draw.");
    $.ajax(req1);
  } else {
    alert("Please assign all teams before saving the draw.");
  }
}