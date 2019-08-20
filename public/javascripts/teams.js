let teams = undefined;
let dtable_editteams = undefined;
let ordering_editteams = undefined;
let searchterm_editteams = undefined;

$("#container_table_editteams").ready(loadTeams);
$("#btn_addteam_submit").click(event => {event.preventDefault(); addTeam()});
$("#btn_editteam_submit").click(event => {event.preventDefault(); editTeam()});

$("#navitem_teams").addClass("text-light");

function loadTeams() {
  $.ajax ({
    type    : "GET",
    url     : app_location + "/api/tab/teams?id=" + tabid,
    headers : {"Authorization" : api_key},
    async   : true,
    success : displayTeams
  });
}

function displayTeams(data) {
  teams = data;
  let table = `
    <table id="table_editteams" class="table table-hover table-bordered">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">ID</th>
          <th class="font-weight-normal">Name</th>
          <th class="font-weight-normal">Delegation</th>
          <th class="font-weight-normal">Status</th>
          <th class="font-weight-normal">Actions</th>
        </tr>
      </thead>
      <tbody>`;
  for(i = 0; i < teams.length; i++) {
    if (teams[i].active == 1) {
      table += "<tr>";
    } else {
      table += '<tr class="bg-suspended">';
    }
    table += "<td>" + teams[i].id + "</td>";
    if (teams[i].active == 1) {
      table += "<td>" + teams[i].name + "</td>";
    } else {
      table += "<td>" + teams[i].name + ' <span class="font-italic">(Suspended)</span></td>';
    }
    table += "<td>" + teams[i].delegation + "</td>";
    table += parseLangStatus(teams[i].status);
    table += '<td class="p-1">';
    table += '<button type="button" class="btn btn-sm btn-outline-secondary btn_team_edit m-1"  data-teamindex="' + i + '">Edit</button>';
    table += '<a target="_blank" class="btn  btn-sm btn-outline-secondary btn_team_showspeakers m-1"  href="' + encodeURI(app_location + "/tab/" + tabid + "/speakers?team=" + teams[i].name) + '">Speakers</a>';
    if (teams[i].active == 1) {
      table += '<button type="button" class="btn  btn-sm btn-outline-secondary btn_team_toggle m-1" data-teamid="' + teams[i].id + '">Suspend</button>';
    } else {
      table += '<button type="button" class="btn  btn-sm btn-secondary btn_team_toggle m-1" data-teamid="' + teams[i].id + '">Activate&nbsp;</button>';
    }
    table += '<button type="button" class="btn  btn-sm btn-danger btn_team_delete m-1" data-teamid="' + teams[i].id + '">Delete</button>';
    table += "</td>";
    table += "</tr>";
  }
  table += `
      </tbody>
    </table>`;
  $("#container_table_editteams").html(table);
  let dtableOptions = {
    stateSave: true,
    paging: false,
    dom: "ft",
    columnDefs: [
      { "searchable": false, "targets": [3, 4] },
      { "sortable": false, "targets": [4] }
    ]
  };
  if (ordering_editteams != undefined) {
    dtableOptions["order"] = ordering_editteams;
  }
  if (searchterm_editteams != undefined && searchterm_editteams != "") {
    dtableOptions["search"] = searchterm_editteams;
  }
  dtable_editteams = $('#table_editteams').DataTable(dtableOptions);
  $(".btn_team_delete").click(deleteTeam);
  $(".btn_team_toggle").click(toggleTeam);
  $(".btn_team_edit").click(showModalEditTeam);
}

function storeTableStateEditTeams() {
  ordering_editteams = dtable_editteams.order();
  searchterm_editteams = dtable_editteams.search();
}

function addTeam() {
  let name = $("#input_addteam_name").val();
  let dele = $("#input_addteam_deleg").val();
  let status = $("#input_addteam_lstatus option:selected").val();
  if (dele == "") {
    dele = name;
  }
  if (name == "") {
    $("#input_addteam_name").addClass("is-invalid");
  } else {
    $.ajax ({
      type    : "POST",
      url     : app_location + "/api/team",
      headers : {"Authorization" : api_key},
      data    : {
        tabid       : tabid,
        name        : name,
        delegation  : dele,
        status      : status
      },
      async   : true,
      success : callbackAddTeam
    });
  }
}

function callbackAddTeam() {
  storeTableStateEditTeams();
  $("#input_addteam_name").val("");
  $("#input_addteam_name").focus();
  loadTeams();
}

function deleteTeam(event) {
  $.ajax ({
    type    : "DELETE",
    url     : app_location + "/api/team?id=" + $(event.target).data("teamid"),
    headers : {"Authorization" : api_key},
    async   : true,
    success : callbackDeleteTeam
  });
}

function callbackDeleteTeam() {
  storeTableStateEditTeams();
  loadTeams();
}

function showModalEditTeam(event) {
  const team = teams[$(event.target).data("teamindex")];
  $("#input_editteam_id").val(team.id);
  $("#input_editteam_name").val(team.name);
  $("#input_editteam_deleg").val(team.delegation);
  $("#input_editteam_lstatus").val(team.status);
  $("#modal_editteam").modal({keyboard: true});
  $("#input_editteam_name").focus();
}

function editTeam() {
  let id = $("#input_editteam_id").val();
  let name = $("#input_editteam_name").val();
  let dele = $("#input_editteam_deleg").val();
  let status = $("#input_editteam_lstatus option:selected").val();
  if (dele == "") {
    $("#input_editteam_deleg").addClass("is-invalid");
  }
  if (name == "") {
    $("#input_editteam_name").addClass("is-invalid");
  } else {
    $.ajax ({
      type    : "PATCH",
      url     : app_location + "/api/team?id=" + id,
      headers : {"Authorization" : api_key},
      data    : {
        name        : name,
        delegation  : dele,
        status      : status
      },
      async   : true,
      success : callbackEditTeam
    });
  }
}

function callbackEditTeam() {
  storeTableStateEditTeams();
  $("#modal_editteam").modal("hide");
  loadTeams();
}

function toggleTeam() {
  $.ajax ({
    type    : "PATCH",
    url     : app_location + "/api/team/toggle?id=" + $(event.target).data("teamid"),
    headers : {"Authorization" : api_key},
    async   : true,
    success : callbackDeleteTeam
  });
}