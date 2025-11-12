import { ref } from 'vue'
export function useWebSocket(url, user) {
  const ws = new WebSocket(url)
  const chatLog = ref([])
  ws.onopen = () => {
    console.log(`${user}连接成功`)
    // ws.send(JSON.stringify('hello' + user))
  }
  ws.onclose = () => {
    console.log(`${user}关闭连接`)
  }
  ws.onerror = () => {
    console.log(`${user}连接出错`)
  }
  ws.onmessage = (e) => {
    e.data.text().then((res) => {
      chatLog.value.push(JSON.parse(res))
      console.log(chatLog.value)
    })
  }
  const send = (msg) => {
    ws.send(JSON.stringify(msg))
  }
  return {
    send,
    chatLog,
  }
}
