import request from '@/utils/request';
import store from "@/store";

const token = store.getters['auth/token']

export const queryKnowledgeGraphic = (params) => request({
    url: "/api/v2/kg/graphic",
    method: "get",
    params: params,
    headers: {
        'Authorization': token
    }
})

export const addKnowledgeGraphicNode = (data) => request({
    url: "/api/v2/kg/node",
    method: "post",
    data,
    headers: {
        'Authorization': token
    }
})

export const deleteKnowledgeGraphicNode = (data) => request({
    url: "/api/v2/kg/node",
    method: "delete",
    data,
    headers: {
        'Authorization': token
    }
})

export const updateKnowledgeGraphicNode = (data) => request({
    url: "/api/v2/kg/node",
    method: "put",
    data,
    headers: {
        'Authorization': token
    }
})

export const getProductNodes = (params) => request({
    url: "/api/v2/kg/nodes",
    method: "get",
    params,
    headers: {
        'Authorization': token
    }
})

export const getNodeInfo = (params) => request({
    url: "/api/v2/kg/node",
    method: "get",
    params,
    headers: {
        'Authorization': token
    }
})

export const getNodeAttributes = (params) => request({
    url: "/api/v2/kg/attr",
    method: "get",
    params,
    headers: {
        'Authorization': token
    }
})

export const addNodeAttribute = (data) => request({
    url: "/api/v2/kg/attr",
    method: "post",
    data,
    headers: {
        'Authorization': token
    }
})

export const deleteNodeAttribute = (data) => request({
    url: "/api/v2/kg/attr",
    method: "delete",
    data,
    headers: {
        'Authorization': token
    }
})