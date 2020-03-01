var store = new Vuex.Store({
  state: {
    api_key: retrieveSession(),
    tabs: [],
    tabid: undefined,
    tabsUpToDate: false,
    permissions: [],
    teams: [],
    teamsUpToDate: false
  },
  getters: {
    tab: state => {
      return state.tabs.find(tab => tab.id == state.tabid)
    },
    signedIn: state => {
      return state.api_key != undefined;
    }
  },
  mutations: {
    setApiKey: (state, x) => {
      state.api_key = x
    },
    setTabs: (state, x) => {
      state.tabs = x
    },
    setTabId: (state, x) => {
      state.tabid = x
    },
    setPermissions: (state, x) => {
      state.permissions = x
    },
    setTabsUpToDate: (state, x) => {
      state.tabsUpToDate = x
    },
    setTeams: (state, x) => {
      state.teams = x
    },
    setTeamsUpToDate: (state, x) => {
      state.teamsUpToDate = x
    },
  }
})