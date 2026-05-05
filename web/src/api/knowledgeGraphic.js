import request from '@/utils/request'

export const queryKnowledgeGraphic = (params) => request({
  url: "/api/v2/kg/graphic",
  method: "get",
  params
})

export const addKnowledgeGraphicNode = (data) => request({
  url: "/api/v2/kg/node",
  method: "post",
  data
})

export const deleteKnowledgeGraphicNode = (data) => request({
  url: "/api/v2/kg/node",
  method: "delete",
  data
})

export const updateKnowledgeGraphicNode = (data) => request({
  url: "/api/v2/kg/node",
  method: "put",
  data
})

export const getProductNodes = (params) => request({
  url: "/api/v2/kg/nodes",
  method: "get",
  params
})

export const getNodeByName = (params) => request({
  url: "/api/v2/kg/node",
  method: "get",
  params
})

export const getNodeInfo = (params) => request({
  url: "/api/v2/kg/node",
  method: "get",
  params
})

export const getNodeAttributes = (params) => request({
  url: "/api/v2/kg/attr",
  method: "get",
  params
})

export const addNodeAttribute = (data) => request({
  url: "/api/v2/kg/attr",
  method: "post",
  data
})

export const deleteNodeAttribute = (data) => request({
  url: "/api/v2/kg/attr",
  method: "delete",
  data
})

export const addRelation = (data) => request({
  url: "/api/v2/kg/relation",
  method: "post",
  data
})

export const deleteRelation = (data) => request({
  url: "/api/v2/kg/relation",
  method: "delete",
  data
})

export const updateRelation = (data) => request({
  url: "/api/v2/kg/relation",
  method: "put",
  data
})

export const getRelationByNodes = (params) => request({
  url: "/api/v2/kg/relationByNodes",
  method: "get",
  params
})

export const enableKnowledgeGraphic = (params) =>
  request({
    url: '/api/v2/user/config',
    method: 'put',
    data: {
      ...params,
      name: 'knowledge_graph.toggle',
      type: 'boolean',
      value: 'true',
      defaultValue: 'false',
      required: true,
      des: 'Knowledge graph toggle',
    },
  })

export const disabledKnowledgeGraphic = (params) =>
  request({
    url: '/api/v2/user/config',
    method: 'put',
    data: {
      ...params,
      name: 'knowledge_graph.toggle',
      type: 'boolean',
      value: 'false',
      defaultValue: 'false',
      required: true,
      des: 'Knowledge graph toggle',
    },
  })

export const getKnowledgeGraphicState = (params) =>
  request({
    url: '/api/v2/user/config/knowledge_graph.toggle',
    method: 'get',
    params
  })

export const addKnowledgeGraphicToggleConfig = (data) => request({
  url: '/api/v2/user/config',
  method: 'post',
  data: {
    ...data,
    name: 'knowledge_graph.toggle',
    type: "boolean",
    value: "false",
    defaultValue: "false",
    required: true,
    des: "Knowledge graph toggle"
  },
})

export const getKnowledgeGraphicForgetState = (params) =>
  request({
    url: '/api/v2/user/config/knowledge_graph.forget.toggle',
    method: 'get',
    params
  })

export const addKnowledgeGraphicForgetToggleConfig = (data) =>
  request({
    url: '/api/v2/user/config',
    method: 'post',
    data: {
      ...data,
      name: 'knowledge_graph.forget.toggle',
      type: 'boolean',
      value: 'false',
      defaultValue: 'false',
      required: true,
      des: 'Knowledge graph forget toggle',
    },
  })

export const knowledgeGraphicForgetToggle = (data) =>
  request({
    url: '/api/v2/user/config',
    method: 'put',
    data: {
      ...data,
      name: 'knowledge_graph.forget.toggle',
      type: 'boolean',
      defaultValue: 'false',
      required: true,
      des: 'Knowledge graph forget toggle',
    },
  })

export const getKnowledgeGraphicForgetEpoch = (params) =>
  request({
    url: '/api/v2/user/config/knowledge_graph.forget.epoch',
    method: 'get',
    params
  })

export const updateKnowledgeGraphicForgetEpoch = (data) =>
  request({
    url: '/api/v2/user/config',
    method: 'put',
    data: {
      ...data,
      name: 'knowledge_graph.forget.epoch',
      type: 'integer',
      defaultValue: '10',
      required: false,
      des: 'Knowledge graph forget epoch',
    },
  })
