import { localRequest } from '@/utils/request'

export const uploadBigFile = (data) =>
  localRequest({
    url: '/upload/bigfile',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    data,
  })
