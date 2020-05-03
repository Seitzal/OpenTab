var store = new Vuex.Store({
  state: {
    api_key: retrieveSession(),
    exp: retrieveSessionExp(),
    tabs: [],
    tabid: undefined,
    tabsUpToDate: false,
    permissions: [],
    teams: [],
    teamsUpToDate: false,
    speakers: [],
    speakersUpToDate: false,
    delegations: [],
    judges: [],
    judgesUpToDate: false,
    clashes: []
  },
  getters: {
    tab: state => {
      return state.tabs.find(tab => tab.id == state.tabid)
    },
    signedIn: state => {
      return state.api_key != undefined;
    },
  },
  mutations: {
    setApiKey: (state, x) => {
      state.api_key = x
    },
    setExp: (state, x) => {
      state.exp = x
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
    setSpeakers: (state, x) => {
      state.speakers = x
    },
    setSpeakersUpToDate: (state, x) => {
      state.speakersUpToDate = x
    },
    setDelegations: (state, x) => {
      state.delegations = x
    },
    setJudges: (state, x) => {
      state.judges = x
    },
    setJudgesUpToDate: (state, x) => {
      state.judgesUpToDate = x
    },
    setClashes: (state, x) => {
      state.clashes = x
    }
  }
})