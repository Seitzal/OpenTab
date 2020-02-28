<template>
  <div>
    <div v-if="tab != undefined" style="display:none">{{tab.id}}</div>
    <v-container>
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
      <table>
        <tbody>
        <tr v-for="team in teams" v-bind:key="team.id">
          <v-card>
            <v-card-title>
              <v-spacer></v-spacer>
              <v-text-field v-model="search" append-icon="mdi-magnify" label="Search" single-line hide-details>
              </v-text-field>
            </v-card-title>
            <v-data-table height="60vh" disable-pagination hide-default-footer fixed-header :loading="!$store.state.teamsUpToDate" 
              loading-text="Loading data..." :headers="headers" :items="$store.state.teams" :search="search" dense>
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
                <v-switch v-model="item.active" mt-0 mb-0 @click.stop="toggleActive(item)" color="primary">
                </v-switch>
              </template>
            </v-data-table>
          </v-card>
        </tr>
      </tbody>
    </table>
  </v-container>
  <v-dialog v-model="dialogEditTeam" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="headline">Update Team</span>
      </v-card-title>
      <v-card-text>
        <v-form v-model="valid">
          <v-container>
            <v-text-field :rules="nameRules" v-model="editedTeam.name" label="Name" required></v-text-field>
            <v-text-field v-model="editedTeam.delegation" label="Delegation"></v-text-field>
            <v-select v-model="editedTeam.status" label="Language Status" :items="statusOptions"></v-select>
          </v-container>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="primary" text @click="editTeamDiscard">Cancel</v-btn>
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
    panel: 0,
    valid: false,
    loading: true,
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
      {text: "Active", value: "active", sortable: false},
      {text: "Actions", value: "action", sortable: false}],
    teams: []
  }},
  computed: {
    signedIn: function() { return this.$store.getters.signedIn },
    tab: function() { return this.$store.getters.tab }
  },
  methods: {
    deleteTeam(team) {
      confirm('Are you sure you want to delete this item?') && {}
    },
    createTeam() {
      
    },
    editTeam(team) {
      
    },
    editTeamDiscard() {
      
    },
    editTeamSave() {

    },
    toggleActive: function(team) {

    }
  },
  watch: {
    tab: function() {
      loadTeams()
    }
  },
  mounted: function() {
    loadTeams()
  }
}
</script>

<style>
</style>