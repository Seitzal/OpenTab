let judges = undefined;
let delegations = undefined;
let dtable_editjudges = undefined;
let ordering_editjudges = undefined;
let searchterm_editjudges = undefined;

$("#container_table_editjudges").ready(loadJudges);
$("#input_addjudge_delegation").ready(loadDelegations);
$("#btn_addjudge_submit").click(event => {event.preventDefault(); addJudge()});
$("#btn_editjudge_submit").click(event => {event.preventDefault(); editJudge()});

$("#navitem_judges").addClass("text-light");

function loadDelegations() {
  let req = rc.getAllDelegations(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = displayDelegations;
  req.error = () => alert("Error loading delegations");
  $.ajax(req);
}

function displayDelegations(data) {
  $("#input_addjudge_delegation").append('<option value="!!!NONE">(independent)</option>');
  delegations = data
  for (i = 0; i < delegations.length; i++) {
    let option = '<option value="' + delegations[i] + '">' + delegations[i] + '</option>';
    $("#input_addjudge_delegation").append(option);
  }

  $("#btn_addjudge_submit").removeAttr("disabled");
}

function loadJudges() {
  let req = rc.getAllJudges(tabid);
  req.headers = {"Authorization" : api_key};
  req.success = displayJudges;
  req.error = () => alert("Error loading judges");
  $.ajax(req);
}

function displayJudges(data) {
  judges = data;
  let table = `
    <table id="table_editjudges" class="table table-hover table-bordered">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">ID</th>
          <th class="font-weight-normal">First Name</th>
          <th class="font-weight-normal">Last Name</th>
          <th class="font-weight-normal">Rating</th>
          <th class="font-weight-normal">Actions</th>
        </tr>
      </thead>
      <tbody>`;
  for(i = 0; i < judges.length; i++) {
    table += "<tr>";
    table += "<td>" + judges[i].id + "</td>";
    table += "<td>" + judges[i].firstName + "</td>";
    table += "<td>" + judges[i].lastName + "</td>";
    table += parseRating(judges[i].rating);
    table += '<td class="p-1">';
    table += '<button type="button" class="btn btn-sm btn-outline-secondary btn_judge_edit m-1"  data-judgeindex="' + i + '">Edit</button>';
    table += '<button type="button" class="btn btn-sm btn-danger btn_judge_delete m-1" data-judgeid="' + judges[i].id + '">Delete</button>';
    table += "</td>";
    table += "</tr>";
  }
  table += `
      </tbody>
    </table>`;
  $("#container_table_editjudges").html(table);
  let dtableOptions = {
    stateSave: true,
    paging: false,
    dom: "ft",
    columnDefs: [
      { "searchable": false, "targets": [4] },
      { "sortable": false, "targets": [4] }
    ]
  };
  if (ordering_editjudges != undefined) {
    dtableOptions["order"] = ordering_editjudges;
  }
  if (searchterm_editjudges != undefined && searchterm_editjudges != "") {
    dtableOptions["search"] = searchterm_editjudges;
  }
  dtable_editjudges = $('#table_editjudges').DataTable(dtableOptions);
  // $(".btn_judge_delete").click(deleteJudge);
  // $(".btn_judge_edit").click(showModalEditJudge);
}

/*
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
*/
