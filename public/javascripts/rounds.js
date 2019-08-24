let rounds = undefined;

$("#tbody_rounds").ready(loadRounds);

$("#navitem_rounds").addClass("text-light");

function loadRounds() {
  $.ajax ({
    type    : "GET",
    url     : app_location + "/api/tab/rounds?id=" + tabid,
    headers : {"Authorization" : api_key},
    async   : true,
    success : displayRounds
  });
}

function displayRounds(data) {
  rounds = data;
  let tbodycontent = "";
  if (rounds.length == 0) {
    tbodycontent = '<tr><td colspan="5">No rounds found.</td></tr>';
  } else {
    for (i = 0; i < rounds.length; i++) {
      tbodycontent += '<tr>';
      tbodycontent += '<td class="w-50">Round ' + rounds[i].roundNumber + '</td>';
      if (rounds[i].finished) {
        tbodycontent += '<td class="bg-info w-25">Completed</td>';
        tbodycontent += '<td><button class="btn btn-outline-primary btn-block">Results</button></td><td></td>';
        if (canSetup) {
          tbodycontent += '<td><button class="btn btn-warning btn-block">Reopen</button></td>';
        } else tbodycontent += '<td></td>';
      } else if (rounds[i].locked) {
        tbodycontent += '<td class="bg-success w-25">Locked</td>';
        tbodycontent += '<td><button class="btn btn-outline-primary btn-block">Results</button></td>';
        if (canSetup) {
          tbodycontent += '<td><button class="btn btn-success btn-block">Complete</button></td>'
          tbodycontent += '<td><button class="btn btn-warning btn-block">Unlock</button></td>';
        } else tbodycontent += '<td></td><td></td>';
      } else {
        tbodycontent += '<td class="bg-warning w-25">Unlocked</td>';
        if (canSetup) {
          tbodycontent += '<td><button class="btn btn-primary btn-block">Set Up</button></td>'
          tbodycontent += '<td><button class="btn btn-success btn-block">Lock</button></td>'
          tbodycontent += '<td><button class="btn btn-danger btn-block">Delete</button></td>';
        } else tbodycontent += '<td></td><td></td><td></td>';
      }
      tbodycontent += '</tr>';
    }
  }
  $("#tbody_rounds").html(tbodycontent);
}