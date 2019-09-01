let speakers = undefined;
let teams = undefined;
let dtable_editspeakers = undefined;
let ordering_editspeakers = undefined;
let searchterm_editspeakers = undefined;
let teamfilter = url_get("team");

$("#container_table_editspeakers").ready(loadSpeakers);
$("#input_addspeaker_team").ready(loadTeams);
$("#btn_addspeaker_submit").click(event => {event.preventDefault(); addSpeaker()});
$("#btn_editspeaker_submit").click(event => {event.preventDefault(); editSpeaker()});

$("#navitem_speakers").addClass("text-light");

function loadTeams() {
  let req = rc.getAllTeams(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = displayTeams;
  req.error = () => alert("Error loading teams");
  $.ajax(req);
}

function displayTeams(data) {
  teams = data
  for (i = 0; i < teams.length; i++) {
    let option = '<option value="' + teams[i].id + '">' + teams[i].name + '</option>';
    if (teamfilter == undefined || teams[i].name == teamfilter)
      $("#input_addspeaker_team").append(option);
  }
  $("#btn_addspeaker_submit").removeAttr("disabled");
}

function loadSpeakers() {
  let req = rc.getAllSpeakers(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = displaySpeakers;
  req.error = () => alert("Error loading speakers");
  $.ajax(req);
}

function displaySpeakers(data) {
  speakers = data;
  let table = `
    <table id="table_editspeakers" class="table table-hover table-bordered">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">ID</th>
          <th class="font-weight-normal">First Name</th>
          <th class="font-weight-normal">Last Name</th>
          <th class="font-weight-normal">Team</th>
          <th class="font-weight-normal">Delegation</th>
          <th class="font-weight-normal">Status</th>
          <th class="font-weight-normal">Actions</th>
        </tr>
      </thead>
      <tbody>`;
  for(i = 0; i < speakers.length; i++) {
    table += "<tr>";
    table += "<td>" + speakers[i].id + "</td>";
    table += "<td>" + speakers[i].firstName + "</td>";
    table += "<td>" + speakers[i].lastName + "</td>";
    table += "<td>" + speakers[i].team + "</td>";
    table += "<td>" + speakers[i].delegation + "</td>";
    table += parseLangStatus(speakers[i].status);
    table += '<td class="p-1">';
    table += '<button type="button" class="btn btn-sm btn-outline-secondary btn_speaker_edit m-1"  data-speakerindex="' + i + '">Edit</button>';
    table += '<button type="button" class="btn btn-sm btn-danger btn_speaker_delete m-1" data-speakerid="' + speakers[i].id + '">Delete</button>';
    table += "</td>";
    table += "</tr>";
  }
  table += `
      </tbody>
    </table>`;
  $("#container_table_editspeakers").html(table);
  let dtableOptions = {
    stateSave: true,
    paging: false,
    dom: "ft",
    columnDefs: [
      { "searchable": false, "targets": [5, 6] },
      { "sortable": false, "targets": [6] }
    ]
  };
  if (ordering_editspeakers != undefined) {
    dtableOptions["order"] = ordering_editspeakers;
  }
  if (searchterm_editspeakers != undefined && searchterm_editspeakers != "") {
    dtableOptions["search"] = searchterm_editspeakers;
  }
  dtable_editspeakers = $('#table_editspeakers').DataTable(dtableOptions);
  if (teamfilter != undefined) {
    dtable_editspeakers.column(3).search(teamfilter).draw();
  } else {
    dtable_editspeakers.column(3).search("").draw();
  }
  $(".btn_speaker_delete").click(deleteSpeaker);
  $(".btn_speaker_edit").click(showModalEditSpeaker);
}

function storeTableStateEditSpeakers() {
  ordering_editspeakers = dtable_editspeakers.order();
  searchterm_editspeakers = dtable_editspeakers.search();
}

function addSpeaker() {
  let firstname = $("#input_addspeaker_firstname").val();
  let lastname = $("#input_addspeaker_lastname").val();
  let teamid = $("#input_addspeaker_team option:selected").val();
  let status = $("#input_addspeaker_lstatus option:selected").val();
  if (firstname == "") {
    $("#input_addspeaker_firstname").addClass("is-invalid");
  } else if (lastname == "") {
    $("#input_addspeaker_lastname").addClass("is-invalid");
  } else {
    let req = rc.createSpeaker();
    req.headers = {"Authorization" : api_key};
    req.data = {
      teamid      : teamid,
      firstname   : firstname,
      lastname    : lastname,
      status      : status
    };
    req.success = () => {
      storeTableStateEditSpeakers();
      $("#input_addspeaker_firstname").val("");
      $("#input_addspeaker_lastname").val("");
      $("#input_addspeaker_firstname").focus();
      loadSpeakers();
    };
    req.error = () => alert("Error adding speaker");
    $.ajax(req);
  }
}

function deleteSpeaker(event) {
  let req = rc.deleteSpeaker($(event.target).data("speakerid"));
  req.headers = {"Authorization" : api_key};
  req.success = () => {
    storeTableStateEditSpeakers();
    loadSpeakers();
  };
  req.error = () => alert("Error removing speaker");
  $.ajax(req);
}

function showModalEditSpeaker(event) {
  const speaker = speakers[$(event.target).data("speakerindex")];
  $("#input_editspeaker_id").val(speaker.id);
  $("#input_editspeaker_team").val(speaker.team);
  $("#input_editspeaker_firstname").val(speaker.firstName);
  $("#input_editspeaker_lastname").val(speaker.lastName);
  $("#input_editspeaker_lstatus").val(speaker.status);
  $("#modal_editspeaker").modal({keyboard: true});
  $("#input_editspeaker_firstname").focus();
}

function editSpeaker() {
  let id = $("#input_editspeaker_id").val();
  let firstname = $("#input_editspeaker_firstname").val();
  let lastname = $("#input_editspeaker_lastname").val();
  let status = $("#input_editspeaker_lstatus option:selected").val();
  if (firstname == "") {
    $("#input_editspeaker_firstname").addClass("is-invalid");
  } else if (lastname == "") {
    $("#input_editspeaker_lastname").addClass("is-invalid");
  } else {
    let req = rc.updateSpeaker(id);
    req.headers = {"Authorization" : api_key};
    req.data = {
      firstname   : firstname,
      lastname    : lastname,
      status      : status
    };
    req.success = () => {
      storeTableStateEditSpeakers();
      $("#modal_editspeaker").modal("hide");
      loadSpeakers();
    };
    req.error = () => alert("Error updating speaker");
    $.ajax(req);
  }
}
