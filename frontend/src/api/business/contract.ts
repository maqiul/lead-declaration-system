import request from '@/utils/request'

/**
 * 分页查询合同模板
 */
export function getTemplates(params: any) {
  return request({
    url: '/v1/contract/templates',
    method: 'get',
    params
  })
}

/**
 * 获取所有启用的合同模板
 */
export function getEnabledTemplates(params?: any) {
  return request({
    url: '/v1/contract/templates/enabled',
    method: 'get',
    params
  })
}

/**
 * 上传合同模板文件
 */
export function uploadTemplate(data: FormData) {
  return request({
    url: '/v1/contract/template/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 生成合同
 */
export function generateContract(templateId: number, declarationFormId: number, data: any) {
  return request({
    url: '/v1/contract/generate',
    method: 'post',
    params: { templateId, declarationFormId },
    data
  })
}

/**
 * 分页查询合同生成记录
 */
export function getGenerations(params: any) {
  return request({
    url: '/v1/contract/generations',
    method: 'get',
    params
  })
}

/**
 * 根据申报单ID获取相关合同
 */
export function getContractsByDeclaration(declarationFormId: number) {
  return request({
    url: `/v1/contract/by-declaration/${declarationFormId}`,
    method: 'get'
  })
}

/**
 * 下载合同文件
 */
export function downloadContract(id: number) {
  // 使用完整的API基础路径构造下载URL
  const downloadUrl = `/api/v1/contract/download/${id}`;
  
  // 直接跳转下载
  window.open(downloadUrl, '_blank');
}

/**
 * 删除合同记录
 */
export function deleteContract(id: number) {
  return request({
    url: `/v1/contract/generation/${id}`,
    method: 'delete'
  })
}

/**
 * 替换合同文件
 */
export function replaceContractFile(id: number, data: FormData) {
  return request({
    url: `/v1/contract/generation/${id}/replace`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
