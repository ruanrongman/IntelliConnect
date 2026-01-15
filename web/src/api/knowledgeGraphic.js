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

export const getProductNodes = (params) => request({
    url: "/api/v2/kg/nodes",
    method: "get",
    params,
    headers: {
        'Authorization': token
    }
})