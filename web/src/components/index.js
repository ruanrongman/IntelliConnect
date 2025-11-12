export function setupComponent(app) {
  const components = import.meta.globEager('./*/index.vue')
  for (let com in components) {
    const comKey = com.match(/\.\/(.*?)\/index\.vue/)[1]
    const comValue = components[com].default
    app.component(comKey, comValue)
  }
}
