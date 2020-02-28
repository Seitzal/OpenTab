<template>
  <v-container>
    <p v-if="tab != undefined">Placeholder: Dashboard for tab "{{ tab.name }}" with id {{ tab.id }}</p>
  </v-container>
</template>

<script>
module.exports = {
  name: "tab",
  computed: {
    tab: function() { return this.$store.getters.tab }
  }
}
</script>

<style>
</style>