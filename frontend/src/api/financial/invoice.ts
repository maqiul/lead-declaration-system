import request from '@/utils/request'

// 财务票据管理API

// 货代发票相关接口
export const financialInvoiceApi = {
  // 分页查询货代发票
  getFreightInvoicePage: (params: any) => {
    return request({
      url: '/financial-invoice/freight/page',
      method: 'get',
      params
    })
  },

  // 查询货代发票详情
  getFreightInvoice: (id: number) => {
    return request({
      url: `/financial-invoice/freight/${id}`,
      method: 'get'
    })
  },

  // 新增货代发票
  addFreightInvoice: (data: any) => {
    return request({
      url: '/financial-invoice/freight',
      method: 'post',
      data
    })
  },

  // 修改货代发票
  updateFreightInvoice: (data: any) => {
    return request({
      url: '/financial-invoice/freight',
      method: 'put',
      data
    })
  },

  // 删除货代发票
  deleteFreightInvoice: (id: number) => {
    return request({
      url: `/financial-invoice/freight/${id}`,
      method: 'delete'
    })
  },

  // 审核货代发票
  auditFreightInvoice: (id: number, status: number, auditRemark?: string) => {
    return request({
      url: `/financial-invoice/freight/${id}/audit`,
      method: 'post',
      params: {
        status,
        auditRemark
      }
    })
  }
}

// 报关发票相关接口（待实现）
export const customsInvoiceApi = {
  // TODO: 实现报关发票相关接口
}

// 开票明细相关接口（待实现）
export const invoiceDetailApi = {
  // TODO: 实现开票明细相关接口
}