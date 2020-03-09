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
            </template><span>reload data</span></v-tooltip>
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
            </template><span>modify</span></v-tooltip>
            <v-tooltip bottom><template v-slot:activator="{ on }">
              <v-icon @click="deleteJudge(item)" v-on="on">
                mdi-delete
              </v-icon>
            </template><span>delete</span></v-tooltip>
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
  </div>
</template>

<script>
module.exports = {
  name: "judges",
  data: function() { return {
    hideControls: false,
    panel: 0,
    valid: false,
    dialogEditJudge: false,
    nameRules: [v => !!v || 'Must enter a name!'],
    ratingRules: [v => {
      const rating = parseInt(this.newJudge.rating, 10)
      if (isNaN(rating)) return "Must enter a number!"
      else if (rating < 0 || rating > 10) return "Must be between 1 and 10!"
      else return true
    }],
    newJudge: {firstName: "", lastName: "", delegation: "(independent)", rating: 5},
    editedJudge: {},
    search: "",
    headers: [
      {text: "ID", value: "id", align: "start"},
      {text: "First Name", value: "firstName"},
      {text: "Last Name", value: "lastName"},
      {text: "Rating", value: "rating"},
      {text: "Active", value: "active"},
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
      createJudge(this.newJudge, () => {
        loadJudges()
        this.newJudge.firstName = ""
        this.newJudge.lastName = ""
      })
    },
    editJudge(judge) {
      this.editedJudge = Object.assign({}, judge);
      this.dialogEditJudge = true;
    },
    editJudgeSave() {
      updateJudge(this.editedJudge, false, () => {
        loadJudges();
        this.dialogEditJudge = false})
    },
    deleteJudge(judge) {
      deleteJudge(judge, loadJudges)
    },
    toggleActive(judge) {
      toggleJudge(judge, loadJudges)
    },
    incrementRating(judge) {
      updateJudge({id: judge.id, rating: judge.rating + 1}, true, loadJudges)
    },
    decrementRating(judge) {
      updateJudge({id: judge.id, rating: judge.rating - 1}, true, loadJudges)
    },
    refresh() {
      loadJudges()
    }
  },
  watch: {
    tab: function() {
      if (this.tab != undefined)
        loadDelegations()
        loadJudges()
    }
  },
  mounted: function() {
    loadDelegations()
    loadJudges()
  }
}
</script>
