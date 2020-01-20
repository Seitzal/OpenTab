let judges = undefined;
let delegations = undefined;
let teams = undefined;
let clashes = undefined;
let judgeindex_clashes = undefined;
let judgeid_clashes = undefined;
let dtable_editjudges = undefined;
let ordering_editjudges = undefined;
let searchterm_editjudges = undefined;

$("#container_table_editjudges").ready(loadJudges);
$("#input_addjudge_delegation").ready(loadDelegations);
$("#input_addclash_team").ready(loadTeams);
$("#btn_addjudge_submit").click(event => {event.preventDefault(); addJudge()});
$("#btn_editjudge_submit").click(event => {event.preventDefault(); editJudge()});
$("#btn_addclash_submit").click(event => {event.preventDefault(); addClash()});

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
    $("#input_addclash_team").append(option);
  }
  $("#btn_addclash_submit").removeAttr("disabled");
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
    table += '<button type="button" class="btn btn-sm btn-outline-secondary btn_judge_clashes m-1"  data-judgeindex="' + i + '">Clashes</button>';
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
  $(".btn_judge_delete").click(deleteJudge);
  $(".btn_judge_edit").click(showModalEditJudge);
  $(".btn_judge_clashes").click(showModalClash);
}

function storeTableStateEditJudges() {
  ordering_editjudges = dtable_editjudges.order();
  searchterm_editjudges = dtable_editjudges.search();
}

function addJudge() {
  let firstname = $("#input_addjudge_firstname").val();
  let lastname = $("#input_addjudge_lastname").val();
  let delegation = $("#input_addjudge_delegation option:selected").val();
  let rating = $("#input_addjudge_rating").val();
  if (firstname == "") {
    $("#input_addjudge_firstname").addClass("is-invalid");
  } else if (lastname == "") {
    $("#input_addjudge_lastname").addClass("is-invalid");
  } else {
    let req = rc.createJudge();
    req.headers = {"Authorization" : api_key};
    req.data = {
      tabid       : tabid,
      firstname   : firstname,
      lastname    : lastname,
      rating      : rating
    };
    if (delegation != "!!!NONE")
      req.data.delegation = delegation
    req.success = () => {
      storeTableStateEditJudges();
      $("#input_addjudge_firstname").val("");
      $("#input_addjudge_lastname").val("");
      $("#input_addjudge_firstname").focus();
      loadJudges();
    };
    req.error = () => alert("Error adding judge");
    $.ajax(req);
  }
}

function deleteJudge(event) {
  let req = rc.deleteJudge($(event.target).data("judgeid"));
  req.headers = {"Authorization" : api_key};
  req.success = () => {
    storeTableStateEditJudges();
    loadJudges();
  };
  req.error = () => alert("Error removing judge");
  $.ajax(req);
}

function showModalEditJudge(event) {
  const judge = judges[$(event.target).data("judgeindex")];
  $("#input_editjudge_id").val(judge.id);
  $("#input_editjudge_firstname").val(judge.firstName);
  $("#input_editjudge_lastname").val(judge.lastName);
  $("#input_editjudge_rating").val(judge.rating);
  $("#modal_editjudge").modal({keyboard: true});
  $("#input_editjudge_firstname").focus();
}

function editJudge() {
  let id = $("#input_editjudge_id").val();
  let firstname = $("#input_editjudge_firstname").val();
  let lastname = $("#input_editjudge_lastname").val();
  let rating = $("#input_editjudge_rating").val();
  if (firstname == "") {
    $("#input_editjudge_firstname").addClass("is-invalid");
  } else if (lastname == "") {
    $("#input_editjudge_lastname").addClass("is-invalid");
  } else {
    let req = rc.updateJudge(id);
    req.headers = {"Authorization" : api_key};
    req.data = {
      firstname   : firstname,
      lastname    : lastname,
      rating      : rating
    };
    req.success = () => {
      storeTableStateEditJudges();
      $("#modal_editjudge").modal("hide");
      loadJudges();
    };
    req.error = () => alert("Error updating judge");
    $.ajax(req);
  }
}

function showModalClash(event) {
  judgeindex_clashes = $(event.target).data("judgeindex");
  loadClashes(judgeindex_clashes);
}

function loadClashes(judgeindex) {
  const name = judges[judgeindex].firstName + " " + judges[judgeindex].lastName;
  $("#title_modal_clashes").html("Clashes for Judge " + name);
  judgeid_clashes = judges[judgeindex].id
  let req = rc.getClashesForJudge(judgeid_clashes);
  req.headers = {"Authorization" : api_key};
  req.success = displayClashes;
  req.error = () => alert("Error loading clashes");
  $.ajax(req);
}

function displayClashes(data) {
  clashes = data;
  let table = `
    <table id="table_editclashes" class="table table-hover table-bordered">
      <thead class="thead-light">
        <tr>
          <th class="font-weight-normal">Team</th>
          <th class="font-weight-normal">Clash Level</th>
          <th class="font-weight-normal"></th>
        </tr>
      </thead>
      <tbody>`;
  for(i = 0; i < clashes.length; i++) {
    table += "<tr>";
    table += "<td>" + clashes[i][0].name + "</td>";
    table += "<td>" + clashes[i][1] + "</td>";
    table += '<td class="p-1">';
    table += '<button type="button" class="btn btn-sm btn-danger btn_clash_delete m-1" data-teamid="' + clashes[i][0].id + '">Delete</button>';
    table += "</td>";
    table += "</tr>";
  }
  table += `
      </tbody>
    </table>`;
  $("#container_table_clashes").html(table);
  $(".btn_clash_delete").click(deleteClash);
  $("#modal_clashes").modal("show");
}

function deleteClash(event) {
  let req = rc.unsetClash(judgeid_clashes, $(event.target).data("teamid"));
  req.headers = {"Authorization" : api_key};
  req.success = () => {
    loadClashes(judgeindex_clashes);
  };
  req.error = () => alert("Error removing clash");
  $.ajax(req);
}

function addClash() {
  let teamid = $("#input_addclash_team option:selected").val();
  let level = $("#input_addclash_level").val();
  let req = rc.setClash(judgeid_clashes, teamid, level);
  req.headers = {"Authorization" : api_key};
  req.success = () => {
    loadClashes(judgeindex_clashes);
  };
  req.error = () => alert("Error adding clash");
  $.ajax(req);
}
