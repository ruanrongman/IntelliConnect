const WS = require('ws')
;((ws) => {
  // debugger
  const server = new WS.Server({ port: 9826 })

  const init = () => {
    bindEvent()
  }

  function bindEvent() {
    server.on('open', handleOpen)
    server.on('close', handleClose)
    server.on('error', handleError)
    server.on('connection', handleConnection)
  }

  function handleOpen() {
    console.log('server websocket open')
  }
  function handleClose() {
    console.log('server websocket close')
  }
  function handleError() {
    console.log('server websocket error')
  }
  function handleConnection(ws) {
    console.log('server websocket connection')
    ws.on('message', handleMessage)
  }
  function handleMessage(msg) {
    // console.log(server)
    // debugger
    server.clients.forEach((c) => {
      //遍历客户端
      // console.log(c)
      c.send(msg) //发送
    })
  }

  init()
  console.log('连接成功')
})(WS)
