$("#container_tabselect").ready(() => {
  let req = rc.getAllTabs();
  if (typeof api_key !== 'undefined')
    req.headers = {"Authorization" : api_key};
  req.success = renderTabs;
  req.error = loadTabsFailure;
  $.ajax (req);
})

function renderTabs(tabs) {
  let buffer = '<table class="table table-hover m-0"><tbody>';
  tabs.forEach(tab => {
    buffer += `
      <tr>
        <td class="p-0">
          <a class="blocklink p-3 text-primary" href="${app_location}/tab/${tab.id}/">${tab.name}</a>
        </td>
      </tr>`;
  });
  buffer += '</tbody></table>';
  $("#container_tabselect").html(buffer);
}

function loadTabsFailure(jqXHR, textStatus, errorThrown) {
  let errMsg = '<div class="m-2 text-danger">Error loading tabs</div>';
  $("#container_tabselect").html(errMsg);
}