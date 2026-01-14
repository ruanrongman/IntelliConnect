import router, { constantRoutes, asyncRoutes } from '@/router'
import { jwtDecode } from "jwt-decode";
import { loginIn } from '@/api/user'
import { start } from 'nprogress'

const state = {
  auth: localStorage.getItem('access-token') || '',
  menuList: [],
}
const getters = {
  token: () => {
    // localStorage.setItem('access-token', state.auth)
    return state.auth
  },
}
const mutations = {
  async GENERATE_ROUTES(state, auth) {
    const layout = constantRoutes.find((item) => item.path === '/')
    const authRoutes = traversalRoutes(asyncRoutes, auth)
    layout.children = [...authRoutes]
    state.menuList = authRoutes
    state.auth = auth
    //router.addRoute(constantRoutes)
    function constantRoutesCheck(routes){
      let result = [];
      routes.forEach(r=>{
        const { name, children } = r;
        if(router.hasRoute(name)) router.removeRoute(name);
        if(children && children.length){
          r.children = constantRoutesCheck(children);
        }
      })
      return result;
    }
    const _constantRoutes = constantRoutesCheck(constantRoutes);
    _constantRoutes.forEach((r) => router.addRoute(r))
  },
  SET_AUTH(state, auth) {
    state.auth = auth
    localStorage.setItem('access-token', auth)
  },
  CLEAR_AUTH(state) {
    localStorage.removeItem('access-token')
    state.auth = ''
  },
}
const actions = {}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions,
}

function traversalRoutes(routes, auth) {
  const result = []
  routes.forEach((r) => {
    let { meta, children, name } = r
    // console.log(`Router Name: ${name}`);
    if(router.hasRoute(name)){
      router.removeRoute(name);
    }
    jwtDecode(auth)
    meta.auth.includes(jwtDecode(auth).role)
    if (meta.auth.includes(jwtDecode(auth).role)) {
      if (children && children.length) {
        r.children = traversalRoutes(children, auth)
      }
      result.push(r)
    }
  })
  return result
}
