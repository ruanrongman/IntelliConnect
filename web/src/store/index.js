import { createStore } from 'vuex'

import app from './app'
import auth from './auth'
const store = createStore({
  strict: true,
  modules: {
    app,
    auth,
  },
})
export default store
