export function setupDirective(app) {
  const directives = import.meta.glob('./*/index.js')
  for (let dire in directives) {
    const direKey = dire.match(/\.\/(.*?)\/index\.js/)[1]

    directives[dire]().then((res) => {
      const direValue = res.default
      app.directive(direKey, direValue)
    })
  }
}
