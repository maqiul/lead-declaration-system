import request from '@/utils/request'

// 获取财务发票列表
export function getFinanceInvoiceList(params: any) {
  return request({
    url: '/v1/finance/invoices',
    method: 'get',
    params
  })
}

// 新增/更新
export function saveFinanceInvoice(data: any) {
  return request({
    url: data.id ? `/v1/finance/invoices/${data.id}` : '/v1/finance/invoices',
    method: data.id ? 'put' : 'post',
    data
  })
}

// 删除
export function deleteFinanceInvoice(id: number) {
  return request({
    url: `/v1/finance/invoices/${id}`,
    method: 'delete'
  })
}

// 上传发票文件
export function uploadInvoiceFile(id: number, data: FormData) {
  return request({
    url: `/v1/finance/invoices/${id}/file`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载发票文件
export function downloadInvoiceFile(id: number) {
  return request({
    url: `/v1/finance/invoices/${id}/file`,
    method: 'get',
    responseType: 'blob'
  })
}
