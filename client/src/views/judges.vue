<template>
  <div>
    <div v-if="tab != undefined" style="display:none">{{tab.id}}</div>
    <v-container v-if="showControls">
      <v-form v-model="valid">
        <v-expansion-panels v-model="panel">
          <v-expansion-panel>
            <v-expansion-panel-header>Add New Judges</v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-row>
                <v-col lg="3" md="4" cols="12">
                  <v-text-field v-model="newJudge.firstName" :rules="nameRules" :counter="50" label="First Name" required></v-text-field>
                </v-col>
                <v-col lg="3" md="4" cols="12">
                  <v-text-field v-model="newJudge.lastName" :rules="nameRules" :counter="50" label="Last Name" required></v-text-field>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-text-field type="number" v-model="newJudge.rating" :rules="ratingRules" label="Rating"></v-text-field>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-select v-model="newJudge.delegation" :items="['(independent)', ...$store.state.delegations]"
                    item-text="name" item-value="id" label="Delegation"></v-select>
                </v-col>
                <v-col lg="2" md="4" cols="12">
                  <v-btn large :disabled="!valid" color="primary" @click="createJudge">Add Judge</v-btn>
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
            </template><span>Reload data</span></v-tooltip>
            <v-switch align="center" dense inset label="Hide Controls" v-if="canSetup" v-model="hideControls"></v-switch>
            <v-spacer></v-spacer>
            <v-text-field class="mt-0" align="center" dense v-model="search" append-icon="mdi-magnify" label="Search" single-line hide-details>
            </v-text-field>
        </v-card-title>
        <v-data-table :height="showControls ? '60vh' : '75vh'" disable-pagination hide-default-footer fixed-header :search="search" dense
          :loading="!$store.state.judgesUpToDate" loading-text="Loading data..." 
          :headers="showControls ? headers : headers.slice(1,3)" 
          :items="$store.state.judges">
          <template v-slot:item.rating="{ item }">
            <v-chip small class="my-1" :color="ratingColor(item.rating)">
              <v-icon small left v-if="item.rating > 0" @click="decrementRating(item)">mdi-minus</v-icon>
              <span class="caption">{{ item.rating }}</span>
              <v-icon small right v-if="item.rating < 10" @click="incrementRating(item)">mdi-plus</v-icon>
            </v-chip>
          </template>
          <template v-slot:item.action="{ item }">
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="editJudge(item)" v-on="on">
                mdi-pencil
              </v-icon>
            </template><span>Modify</span></v-tooltip>
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="editClashes(item)" v-on="on">
                mdi-eye-off
              </v-icon>
            </template><span>View/edit clashes</span></v-tooltip>
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="deleteJudge(item)" v-on="on">
                mdi-delete
              </v-icon>
            </template><span>Delete</span></v-tooltip>
          </template>
          <template v-slot:item.isActive="{ item }">
            <v-icon v-if="item.isActive" @click="toggleActive(item)" color="primary">
              mdi-checkbox-marked
            </v-icon>
            <v-icon v-if="!item.isActive" @click="toggleActive(item)">
              mdi-checkbox-blank-outline
            </v-icon>
          </template>
        </v-data-table>
      </v-card>
    </v-container>
    <v-dialog v-model="dialogEditJudge" max-width="500px" @keydown.enter="editJudgeSave()" @keydown.esc="dialogEditJudge = false;">
      <v-card>
        <v-card-title>
          <span class="headline">Update Judge</span>
        </v-card-title>
        <v-card-text>
          <v-form v-model="valid">
            <v-container>
              <v-text-field :rules="nameRules" v-model="editedJudge.firstName" label="First Name" required autofocus></v-text-field>
              <v-text-field :rules="nameRules" v-model="editedJudge.lastName" label="Last Name" required autofocus></v-text-field>
            </v-container>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" text @click="dialogEditJudge = false;">Cancel</v-btn>
          <v-btn color="primary" text @click="editJudgeSave">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-dialog v-model="dialogClashes" max-width="700px">
      <v-card>
        <v-card-title>
          <span class="headline">Clashes for {{editedJudge.firstName}} {{editedJudge.lastName}}</span>
        </v-card-title>
        <v-card-text>
          <v-simple-table>
            <template v-slot:default>
              <thead>
                <tr>
                  <th class="text-left">Team</th>
                  <th class="text-left">Clash Level</th>
                  <th style="width: 40px;"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="clash in $store.state.clashes" :key="clash[0].id">
                  <td>{{$store.getters.teamName(clash[0])}}</td>
                  <td>
                    <v-chip small class="my-1" :color="ratingColor(10 - clash[1])">
                      <v-icon small left v-if="clash[1] > 0" @click="setClash(editedJudge.id, clash[0], clash[1] - 1)">mdi-minus</v-icon>
                      <span class="caption">{{ clash[1] }}</span>
                      <v-icon small right v-if="clash[1] < 10" @click="setClash(editedJudge.id, clash[0], clash[1] + 1)">mdi-plus</v-icon>
                    </v-chip>
                  </td>
                  <td>
                    <v-tooltip bottom><template v-slot:activator="{ on }">
                      <v-icon @click="unclash(editedJudge.id, clash[0])" v-on="on">
                        mdi-delete
                      </v-icon>
                    </template><span>Unclash</span></v-tooltip>
                  </td>
                </tr>
              </tbody>
              <tfoot>
                <v-form>
                  <tr>
                    <td>
                      <v-select v-model="newClash.teamid" :items="$store.state.teams" 
                        :rules="teamRules" item-text="name" item-value="id" label="Team" required></v-select>
                    </td>
                    <td>
                      <v-text-field type="number" v-model="newClash.level" :rules="ratingRules" label="Level">
                      </v-text-field>
                    </td>
                    <td>
                      <v-btn color="primary" @click="setClash(editedJudge.id, newClash.teamid, newClash.level)">
                        <v-icon>mdi-plus-thick</v-icon>
                      </v-btn>
                    </td>
                  </tr>
                </v-form>
              </tfoot>
            </template>
          </v-simple-table>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script>
import api from '../api.js';

export default {
  name: "judges",
  data: function() { return {
    hideControls: false,
    panel: 0,
    valid: false,
    dialogEditJudge: false,
    dialogClashes: false,
    nameRules: [v => !!v || 'Must enter a name!'],
    teamRules: [v => !!v || 'Must select a team!'],
    ratingRules: [v => {
      const rating = parseInt(this.newJudge.rating, 10)
      if (isNaN(rating)) return "Must enter a number!"
      else if (rating < 0 || rating > 10) return "Must be between 1 and 10!"
      else return true
    }],
    newJudge: {firstName: "", lastName: "", delegation: "(independent)", rating: 5},
    editedJudge: {},
    newClash: {teamid: -1, level: 10},
    search: "",
    headers: [
      {text: "ID", value: "id", align: "start"},
      {text: "First Name", value: "firstName"},
      {text: "Last Name", value: "lastName"},
      {text: "Rating", value: "rating"},
      {text: "Active", value: "isActive"},
      {text: "Actions", value: "action", sortable: false}]
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
    ratingColor(rating) {
      return ratingColors[rating];
    },
    createJudge() {
      api.createJudge(this.newJudge, () => {
        api.loadJudges()
        this.newJudge.firstName = ""
        this.newJudge.lastName = ""
      })
    },
    editJudge(judge) {
      this.editedJudge = Object.assign({}, judge);
      this.dialogEditJudge = true;
    },
    editJudgeSave() {
      api.updateJudge(this.editedJudge, false, () => {
        api.loadJudges();
        this.dialogEditJudge = false})
    },
    deleteJudge(judge) {
      confirm('Are you sure you want to delete this item?') && api.deleteJudge(judge, api.loadJudges)
    },
    toggleActive(judge) {
      api.toggleJudge(judge, api.loadJudges)
    },
    incrementRating(judge) {
      api.updateJudge({id: judge.id, rating: judge.rating + 1}, true, api.loadJudges)
    },
    decrementRating(judge) {
      api.updateJudge({id: judge.id, rating: judge.rating - 1}, true, api.loadJudges)
    },
    refresh() {
      api.loadJudges()
    },
    editClashes(judge) {
      this.newClash.teamid = this.$store.state.teams[0].id
      this.editedJudge = Object.assign({}, judge);
      api.loadClashes(judge, () => {this.dialogClashes = true});
    },
    unclash(judgeid, teamid) {
      api.deleteClash(judgeid, teamid, 
        () => {api.loadClashes(this.editedJudge, () => {})})
    },
    setClash(judgeid, teamid, level) {
      api.setClash(judgeid, teamid, level, 
        () => {api.loadClashes(this.editedJudge, () => {})})
    }
  },
  watch: {
    tab: function() {
      if (this.tab != undefined)
        api.loadTeams()
        api.loadDelegations()
        api.loadJudges()
    }
  },
  mounted: function() {
    api.loadTeams()
    api.loadDelegations()
    api.loadJudges()
  }
}
</script>