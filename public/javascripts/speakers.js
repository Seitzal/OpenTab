let speakers = undefined;
let teams = undefined;
let dtable_editspeakers = undefined;
let ordering_editspeakers = undefined;
let searchterm_editspeakers = undefined;

$("#container_table_editspeakers").ready(loadSpeakers);
$("#input_addspeaker_team").ready(loadTeams);
$("#btn_addspeaker_submit").click(event => {event.preventDefault(); addSpeaker()});
$("#btn_editspeaker_submit").click(event => {event.preventDefault(); editSpeaker()});

$("#navitem_speakers").addClass("text-light");

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
  teams = data
  for (i = 0; i < teams.length; i++) {
    let option = '<option value="' + teams[i].id + '">' + teams[i].name + '</option>';
    $("#input_addspeaker_team").append(option);
  }
  $("#btn_addspeaker_submit").removeAttr("disabled");
}

function loadSpeakers() {
  $.ajax ({
    type    : "GET",
    url     : app_location + "/api/tab/speakers?id=" + tabid,
    headers : {"Authorization" : api_key},
    async   : true,
    success : displaySpeakers
  });
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
    table += '<td class="p-1 bg-light">';
    table += '<button type="button" class="btn btn-outline-secondary btn_speaker_edit ml-1"  data-speakerindex="' + i + '">Edit</button>';
    table += '<button type="button" class="btn btn-danger btn_speaker_delete ml-1" data-speakerid="' + speakers[i].id + '">Delete</button>';
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
    $.ajax ({
      type    : "POST",
      url     : app_location + "/api/speaker",
      headers : {"Authorization" : api_key},
      data    : {
        teamid      : teamid,
        firstname   : firstname,
        lastname    : lastname,
        status      : status
      },
      async   : true,
      success : callbackAddSpeaker
    });
  }
}

function callbackAddSpeaker() {
  storeTableStateEditSpeakers();
  $("#input_addspeaker_firstname").val("");
  $("#input_addspeaker_lastname").val("");
  loadSpeakers();
}

function deleteSpeaker(event) {
  $.ajax ({
    type    : "DELETE",
    url     : app_location + "/api/speaker?id=" + $(event.target).data("speakerid"),
    headers : {"Authorization" : api_key},
    async   : true,
    success : callbackDeleteSpeaker
  });
}

function callbackDeleteSpeaker() {
  storeTableStateEditSpeakers();
  loadSpeakers();
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
    $.ajax ({
      type    : "PATCH",
      url     : app_location + "/api/speaker?id=" + id,
      headers : {"Authorization" : api_key},
      data    : {
        firstname   : firstname,
        lastname    : lastname,
        status      : status
      },
      async   : true,
      success : callbackEditSpeaker
    });
  }
}

function callbackEditSpeaker() {
  storeTableStateEditSpeakers();
  $("#modal_editspeaker").modal("hide");
  loadSpeakers();
}