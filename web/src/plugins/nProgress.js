import NProgress from "nprogress"
import "nprogress/nprogress.css"

NProgress.configure({
  // easing: 'ease', // 动画方式    
  // speed: 500, // 递增进度条的速度    
  // showSpinner: true, // 是否显示加载ico    
  // trickleSpeed: 200, // 自动递增间隔    
  // minimum: 0.3 // 
  minimum:0.1 ,// 启动时的最小百分比
  easing:'ease',// 进度条加载方式 ，先慢后快在慢
  speed:600, // 加载速度
  showSpinner:false//右侧加载图标
  // parent:'body' // 加载的容器
})

export default NProgress