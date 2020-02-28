<template>
  <v-container>
    <p v-if="tab != undefined">Placeholder: Teams view for tab "{{ tab.name }}" with id {{ tab.id }}</p>
  </v-container>
</template>

<script>
module.exports = {
  name: "teams",
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