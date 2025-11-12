export default class DCIns {
  constructor(viewer) {
    this.viewer = viewer
  }

  addMarker(point) {
    let layer = this.viewer.getLayer('layer') //切换时清除上一个图层 避免多于点污染
    if (layer) {
      layer.clear()
    } else {
      layer = new window.DC.VectorLayer('layer')
    }

    this.viewer.addLayer(layer)
    let position = new window.DC.Point(
      window.DC.Position.fromObject({
        ...point,
      })
    )
    let _this = layer.addOverlay(position)
    this.viewer.flyTo(layer)
    return _this
  }
  viewOnePlace(point) {
    let layer = this.viewer.getLayer('layer')
    if (layer) {
      layer.clear()
    } else {
      layer = new window.DC.VectorLayer('layer')
    }
    let terrain = window.DC.TerrainFactory.createUrlTerrain({
      url: 'http://data.marsgis.cn/terrain',
    })
    this.viewer.addTerrain(terrain)
    this.viewer.flyToPosition(
      window.DC.Position.fromObject({
        ...point,
      })
    )
  }
}
