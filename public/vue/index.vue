<template>
  <v-container>
    <v-card  v-if="$root.signedIn" class="mb-4">
      <v-card-title>
        Welcome,&nbsp;{{$root.username()}}!
      </v-card-title>
    </v-card>
    <v-expansion-panels v-model="panel">
      <v-expansion-panel>
        <v-expansion-panel-header v-if="$root.signedIn" class="title">Your Tabs</v-expansion-panel-header>
        <v-expansion-panel-header v-if="!$root.signedIn" class="title">Public Tabs</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-list v-if="$root.tabsUpToDate">
            <v-list-item v-for="tab in $root.tabs" :key="tab.id" :to="'tab/' + tab.id">
              <v-list-item-icon><v-icon>mdi-arrow-right-thick</v-icon></v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title>{{tab.name}}</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list>
          <v-container class="text-center pt-0" v-if="!$root.tabsUpToDate">
            <v-progress-circular indeterminate color="primary"></v-progress-circular>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-container>
</template>

<script>
module.exports = {
  name: "index",
  data: function() { return {
    panel: 0
  }},
}
</script>

<style>
</style>