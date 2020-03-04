<template>
  <div>
    <div v-if="tab != undefined" style="display:none">{{tab.id}}</div>
    <v-container v-if="teamid != undefined">
      <v-card>
        <v-toolbar>
            <div>Showing speakers for team "{{teamname}}"</div><v-spacer></v-spacer><v-btn text to="./">show all</v-btn>
        </v-toolbar>
      </v-card>
    </v-container>
    <v-container v-if="showControls">
      <v-form v-model="valid">
        <v-expansion-panels v-model="panel">
          <v-expansion-panel>
            <v-expansion-panel-header>Add New Speakers</v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-row>
                <v-col lg="3" md="4" cols="12">
                  <v-text-field v-model="newSpeaker.firstName" :rules="nameRules" :counter="50" label="First Name" required></v-text-field>
                </v-col>
                <v-col lg="3" md="4" cols="12">
                  <v-text-field v-model="newSpeaker.lastName" :rules="nameRules" :counter="50" label="Last Name" required></v-text-field>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-select v-model="newSpeaker.teamid" :disabled="teamid != undefined" :items="teams" :rules="teamRules"
                    item-text="name" item-value="id" label="Team" required></v-select>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-btn-toggle v-model="newSpeaker.status" mandatory>
                    <v-btn value="1">EPL</v-btn>
                    <v-btn value="2">ESL</v-btn>
                    <v-btn value="3">EFL</v-btn>
                  </v-btn-toggle>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-btn large :disabled="!valid" color="primary" @click="createSpeaker">Add Speaker</v-btn>
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
          <v-tooltip bottom><template v-slot:activator="{ on }">
            <v-btn dense text class="mr-3" align="center" @click="refresh" v-on="on"><v-icon>mdi-refresh</v-icon></v-btn>
          </template><span>reload data</span></v-tooltip>
          <v-switch align="center" dense inset label="Hide Controls" v-if="canSetup" v-model="hideControls"></v-switch>
          <v-spacer></v-spacer>
          <v-text-field class="mt-0" align="center" dense v-model="search" append-icon="mdi-magnify" label="Search" single-line hide-details>
          </v-text-field>
        </v-card-title>
        <v-data-table :height="showControls ? '60vh' : '75vh'" disable-pagination hide-default-footer fixed-header :search="search" dense
          :loading="!$store.state.speakersUpToDate" loading-text="Loading data..." 
          :headers="showControls ? headers : headers.slice(1,5)" 
          :items="speakers">
          <template v-slot:item.status="{ item }">
            {{ statusOptions.find(s => s.value == item.status).text }}
          </template>
          <template v-slot:item.action="{ item }">
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="editSpeaker(item)" v-on="on">
                mdi-pencil
              </v-icon>
            </template><span>modify</span></v-tooltip>
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="deleteSpeaker(item)" v-on="on">
                mdi-delete
              </v-icon>
            </template><span>modify</span></v-tooltip>
          </template>
        </v-data-table>
      </v-card>
    </v-container>
    <v-dialog v-model="dialogEditSpeaker" max-width="500px" @keydown.enter="editSpeakerSave()" @keydown.esc="dialogEditSpeaker = false">
      <v-card>
        <v-card-title>
          <span class="headline">Update Speaker</span>
        </v-card-title>
        <v-card-text>
          <v-form v-model="valid">
            <v-container>
              <v-text-field :rules="nameRules" v-model="editedSpeaker.firstName" label="First Name" required autofocus></v-text-field>
              <v-text-field :rules="nameRules" v-model="editedSpeaker.lastName" label="Last Name" required autofocus></v-text-field>
              <v-select v-model="editedSpeaker.status" label="Language Status" :items="statusOptions"></v-select>
            </v-container>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" text @click="dialogEditSpeaker = false">Cancel</v-btn>
          <v-btn color="primary" text @click="editSpeakerSave">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
module.exports = {
  name: "speakers",
  data: function() { return {
    hideControls: false,
    panel: 0,
    valid: false,
    dialogEditSpeaker: false,
    nameRules: [v => !!v || 'Must enter a name!'],
    teamRules: [v => !!v || 'Must select a team!'],
    statusOptions: [{text: "EPL", value: 1},{text: "ESL", value: 2},{text: "EFL", value: 3}],
    newSpeaker: {},
    editedSpeaker: {},
    search: "",
    headers: [
      {text: "ID", value: "id", align: "start"},
      {text: "First Name", value: "firstName"},
      {text: "Last Name", value: "lastName"},
      {text: "Team", value: "team"},
      {text: "Language Status", value: "status"},
      {text: "Actions", value: "action", sortable: false}],
  }},
  computed: {
    signedIn: function() { return this.$store.getters.signedIn },
    tab: function() { return this.$store.getters.tab },
    canSetup: function() {
      if (this.$store.state.permissions.length != 0 && this.tab != undefined)
        return this.$store.state.permissions.find(set => set[0] == this.tab.id)[1].setup
      else return false
    },
    showControls: function() { return this.canSetup && !this.hideControls },
    teamid: function() { return this.$route.params.teamid },
    teamname: function() {
      if (this.teamid != undefined) {
        const team = this.$store.state.teams.find(team => team.id == this.teamid)
        if (team != undefined) {
          return team.name
        } else {
          return undefined
        }
      }
     },
    speakers: function() {
      if (this.teamid != undefined) {
        return this.$store.state.speakers.filter(speaker => speaker.teamid == this.teamid)
      } else {
        return this.$store.state.speakers
      }
    },
    teams: function() {
      if (this.teamname != undefined) {
        return [{name: this.teamname, id: this.teamid}]
      } else return this.$store.state.teams
    },
  },
  methods: {
    deleteSpeaker(speaker) {
      confirm('Are you sure you want to delete this item?') && deleteSpeaker(speaker, loadSpeakers)
    },
    createSpeaker() {
      createSpeaker(this.newSpeaker, loadSpeakers)
    },
    editSpeaker(speaker) {
      this.editedSpeaker = Object.assign({}, speaker);
      this.dialogEditSpeaker = true;
    },
    refresh() { 
      loadSpeakers() 
    },
    editSpeakerSave() {
      updateSpeaker(this.editedSpeaker, () => {loadSpeakers(); this.dialogEditSpeaker = false})
    },
  },
  watch: {
    tab: function() {
      if (this.tab != undefined)
        loadTeams()
        loadSpeakers()
    },
    teamid: function() {
      this.newSpeaker.teamid = this.teamid
    }
  },
  mounted: function() {
    loadTeams()
    loadSpeakers()
    this.newSpeaker.teamid = this.teamid
  }
}
</script>
