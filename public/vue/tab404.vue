<template>
  <v-card class="sharpcorners">
    <v-card-title style="background-color: #EF5350;">
      <span class="headline">Error 404: Tab not found</span>
    </v-card-title>
    <v-card-text class="mt-4 subheading">
      The tab you were looking for does not exist on this server.
      Maybe it was deleted, or you have the wrong ID?
    </v-card-text>
  </v-card>
</template>

<script>
module.exports = {
  name: "tab404",
  data: function() { return {
  }},
}
</script>

<style>
</style>