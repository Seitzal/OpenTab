$("#container_tabselect").ready(() => {
  $.ajax ({
    type    : "GET",
    url     : app_location + "/api/tabs",
    headers : (typeof api_key === 'undefined') ? {} : {"Authorization" : api_key},
    async   : true,
    success : renderTabs
  });
})

function renderTabs(tabs) {
  let buffer = '<table class="table table-hover"><tbody>';
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