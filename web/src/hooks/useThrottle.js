export const useThrottle = (func, wait) => {
  let ctx, args, result, timeoutID // caching
  var last = 0
  const call = () => {
    timeoutID = 0
    last = +new Date()
    result = func.apply(ctx, args)
    ctx = null
    args = null
  }

  return function (...argument) {
    ctx = this
    args = argument
    // args = a
    var delta = new Date() - last
    if (!timeoutID)
      if (delta >= wait) call()
      else timeoutID = setTimeout(call, wait - delta)
    return result
  }
}
