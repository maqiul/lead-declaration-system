Fesod Excel模板使用说明
=====================

请在此目录下创建 template.xlsx 文件，模板格式如下：

第一行（标题行）：
A1: COMMERCIAL INVOICE

第二行（发票号行）：
A2: Invoice No.: {invoiceNo}        E2: Date: {date}

第三行（发货人信息）：
A3: Seller: {shipperCompany}
A4: {shipperAddress}

第五行（收货人信息）：
A5: Buyer: {consigneeCompany}
A6: {consigneeAddress}

第八行（表头）：
A8: HS Code    B8: Product Name    C8: Quantity    D8: Unit    E8: Unit Price    F8: Amount

第九行开始（产品数据）：
使用Fesod的列表填充功能自动填充产品信息

注意事项：
1. 使用 {} 包围变量名作为占位符
2. 产品列表会自动从第9行开始垂直填充
3. 可以在模板中设置单元格格式、边框、字体等样式
4. 合并单元格会在填充时自动处理

示例占位符：
{invoiceNo} - 发票号
{date} - 日期
{shipperCompany} - 发货人公司
{shipperAddress} - 发货人地址
{consigneeCompany} - 收货人公司
{consigneeAddress} - 收货人地址
{totalAmount} - 总金额
{totalAmountWords} - 总金额大写