<template>
  <v-container>
    <p v-if="tab != undefined">Placeholder: Dashboard for tab "{{ tab.name }}" with id {{ tab.id }}</p>
  </v-container>
</template>

<script>
module.exports = {
  name: "tab",
  data: function() { return {
  }},
  computed: {
    tab: function() {
      const tab = this.$root.tabs.find(tab => tab.id == this.$route.params.tabid)
      this.$root.tab = tab
      return tab
    }
  },
}
</script>

<style>
</style>