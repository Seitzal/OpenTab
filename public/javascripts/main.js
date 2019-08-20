function parseLangStatus(langstatus) {
  switch(langstatus) {
    case 1:
      return '<td class="text-body table-primary">EPL</td>';
    case 2:
      return '<td class="text-body table-info">ESL</td>';
    case 3:
      return '<td class="text-body table-success">EFL</td>';
    default:
      return '<td class="text-body table-secondary">Unknown</td>';
  }
}
