<template>
  <div>
    <div v-if="tab != undefined" style="display:none">{{tab.id}}</div>
    <v-container v-if="showControls">
      <v-form v-model="valid">
        <v-expansion-panels v-model="panel">
          <v-expansion-panel>
            <v-expansion-panel-header>Add New Teams</v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-row>
                <v-col cols="12" sm="4">
                  <v-text-field v-model="newTeam.name" :rules="nameRules" :counter="50" label="Team Name" required></v-text-field>
                </v-col>
                <v-col cols="12" sm="4">
                  <v-text-field v-model="newTeam.delegation" :counter="50" label="Delegation"></v-text-field>
                </v-col>
                <v-col cols="12" sm="4">
                  <v-btn-toggle class="mb-4" v-model="newTeam.status" mandatory>
                    <v-btn value="1">EPL</v-btn>
                    <v-btn value="2">ESL</v-btn>
                    <v-btn value="3">EFL</v-btn>
                  </v-btn-toggle>
                  <v-btn large class="ml-4" :disabled="!valid" color="primary" @click="createTeam">Add Team</v-btn>
                </v-col>
              </v-row>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-form>
    </v-container>
    <v-container>
      <v-card>
        <v-card-title class="py-0">
            <v-btn dense text class="mr-3" align="center" @click="refresh"><v-icon>mdi-refresh</v-icon></v-btn>
            <v-switch align="center" dense inset label="Hide Controls" v-if="canSetup" v-model="hideControls"></v-switch>
            <v-spacer></v-spacer>
            <v-text-field class="mt-0" align="center" dense v-model="search" append-icon="mdi-magnify" label="Search" single-line hide-details>
            </v-text-field>
        </v-card-title>
        <v-data-table :height="showControls ? '60vh' : '75vh'" disable-pagination hide-default-footer fixed-header :search="search" dense
          :loading="!$store.state.teamsUpToDate" loading-text="Loading data..." 
          :headers="showControls ? headers : headers.slice(1,4)" 
          :items="$store.state.teams">
          <template v-slot:item.status="{ item }">
            {{ statusOptions.find(s => s.value == item.status).text }}
          </template>
          <template v-slot:item.action="{ item }">
            <v-icon class="mr-2" @click="editTeam(item)">
              mdi-pencil
            </v-icon>
            <v-icon @click="deleteTeam(item)">
              mdi-delete
            </v-icon>
          </template>
          <template v-slot:item.active="{ item }">
            <v-icon v-if="item.active" @click="toggleActive(item)" color="primary">
              mdi-checkbox-marked
            </v-icon>
            <v-icon v-if="!item.active" @click="toggleActive(item)">
              mdi-checkbox-blank-outline
            </v-icon>
          </template>
        </v-data-table>
      </v-card>
    </v-container>
    <v-dialog v-model="dialogEditTeam" max-width="500px" @keydown.enter="editTeamSave()" @keydown.esc="dialogEditTeam = false;">
      <v-card>
        <v-card-title>
          <span class="headline">Update Team</span>
        </v-card-title>
        <v-card-text>
          <v-form v-model="valid">
            <v-container>
              <v-text-field :rules="nameRules" v-model="editedTeam.name" label="Name" required autofocus></v-text-field>
              <v-text-field v-model="editedTeam.delegation" label="Delegation"></v-text-field>
              <v-select v-model="editedTeam.status" label="Language Status" :items="statusOptions"></v-select>
            </v-container>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" text @click="dialogEditTeam = false;">Cancel</v-btn>
          <v-btn color="primary" text @click="editTeamSave">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
module.exports = {
  name: "teams",
  data: function() { return {
    hideControls: false,
    panel: 0,
    valid: false,
    dialogEditTeam: false,
    nameRules: [v => !!v || 'Must enter a name!'],
    statusOptions: [{text: "EPL", value: 1},{text: "ESL", value: 2},{text: "EFL", value: 3}],
    newTeam: {name: "", delegation: "", status: 1},
    editedTeam: {id: 0, tabid: 0, name: "", delegation: "", status: 1, active: true},
    search: "",
    headers: [
      {text: "ID", value: "id", align: "start"},
      {text: "Name", value: "name"},
      {text: "Delegation", value: "delegation"},
      {text: "Language Status", value: "status"},
      {text: "Active", value: "active"},
      {text: "Actions", value: "action", sortable: false}],
    teams: []
  }},
  computed: {
    signedIn: function() { return this.$store.getters.signedIn },
    tab: function() { return this.$store.getters.tab },
    canSetup: function() {
      if (this.$store.state.permissions.length != 0 && this.tab != undefined)
        return this.$store.state.permissions.find(set => set[0] == this.tab.id)[1].setup
      else return false
    },
    showControls: function() { return this.canSetup && !this.hideControls }
  },
  methods: {
    deleteTeam(team) {
      confirm('Are you sure you want to delete this item?') && deleteTeam(team, loadTeams)
    },
    createTeam() {
      createTeam(this.newTeam, loadTeams)
    },
    editTeam(team) {
      this.editedTeam = Object.assign({}, team);
      this.dialogEditTeam = true;
    },
    editTeamSave() {
      updateTeam(this.editedTeam, () => {loadTeams(); this.dialogEditTeam = false})
    },
    toggleActive(team) {
      toggleTeam(team, loadTeams)
    },
    refresh() {
      loadTeams()
    }
  },
  watch: {
    tab: function() {
      if (this.tab != undefined)
        loadTeams()
    }
  },
  mounted: function() {
    loadTeams()
  }
}
</script>
