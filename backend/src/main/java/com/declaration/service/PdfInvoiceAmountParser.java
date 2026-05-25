package com.declaration.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发票 PDF 金额解析服务。
 * <p>
 * 基于 Apache PDFBox 提取 PDF 中的文字，再用多组正则按优先级匹配发票金额。
 * 仅支持"文本型"PDF（电子发票、可复制文字的 PDF），扫描件/图片类 PDF 无法解析。
 * </p>
 */
@Slf4j
@Service
public class PdfInvoiceAmountParser {

    /** 单个解析结果 */
    @Data
    public static class PdfParseResult {
        /** 解析出的金额（可能为 null） */
        private BigDecimal amount;
        /** 是否成功解析出金额 */
        private boolean success;
        /** 失败原因（仅 success=false 时有值） */
        private String errorMsg;
        /** 提取的文字片段（仅调试用，截取前 500 字符） */
        private String textSnippet;
    }

    /** 最多读取前 N 页（避免大文件性能问题） */
    private static final int MAX_PAGES = 3;

    // ---------------- 正则（按优先级） ----------------

    /** 优先级 1：价税合计 / 合计金额 / 价税合计（小写）/ 总计 */
    private static final Pattern P_TOTAL_CN = Pattern.compile(
            "(?:价税合计|合计金额|价税合计\\s*（小写）|总计|小写)[^\\d\\-]{0,20}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** 优先级 2：货币符号 + 数字（¥/￥/CNY/USD） */
    private static final Pattern P_CURRENCY = Pattern.compile(
            "(?:[¥￥]|CNY|USD|RMB)[\\s\\u00a0]{0,5}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);

    /** 优先级 3：Total Amount / Grand Total / Total */
    private static final Pattern P_TOTAL_EN = Pattern.compile(
            "(?:Total\\s*Amount|Grand\\s*Total|Amount\\s*Due|Net\\s*Amount|Total)[^\\d\\-]{0,25}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    /** 优先级 4：以 ".xx" 结尾的两位小数金额（兜底，取最大值作为候选） */
    private static final Pattern P_DECIMAL = Pattern.compile(
            "(-?[\\d]{1,3}(?:,[\\d]{3})*)\\.([\\d]{2})(?![\\d])");

    /**
     * 解析 PDF 文件中的发票金额。
     *
     * @param input PDF 输入流，调用方负责关闭
     * @return 解析结果（不为 null）
     */
    public PdfParseResult parseAmount(InputStream input) {
        PdfParseResult result = new PdfParseResult();
        try (PDDocument doc = PDDocument.load(input)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = doc.getNumberOfPages();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(totalPages, MAX_PAGES));
            String text = stripper.getText(doc);

            if (text == null || text.trim().isEmpty()) {
                result.setSuccess(false);
                result.setErrorMsg("无法提取文字，请确认 PDF 非扫描件");
                return result;
            }
            result.setTextSnippet(text.length() > 500 ? text.substring(0, 500) : text);

            // 按优先级匹配
            BigDecimal amount = tryMatch(text, P_TOTAL_CN);
            if (amount == null) amount = tryMatch(text, P_CURRENCY);
            if (amount == null) amount = tryMatch(text, P_TOTAL_EN);
            if (amount == null) amount = pickMaxDecimal(text);

            if (amount == null) {
                result.setSuccess(false);
                result.setErrorMsg("未在 PDF 中识别到金额");
            } else {
                result.setAmount(amount);
                result.setSuccess(true);
            }
            return result;
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            log.warn("PDF 加密无法解析: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMsg("PDF 已加密，无法自动识别金额");
            return result;
        } catch (Exception e) {
            log.warn("解析 PDF 金额异常: {}", e.getMessage());
            result.setSuccess(false);
            result.setErrorMsg("非有效 PDF 文件");
            return result;
        }
    }

    // ---------------- 内部工具 ----------------

    /** 正则命中后，把捕获组 1 中的逗号去掉再解析 BigDecimal */
    private BigDecimal tryMatch(String text, Pattern p) {
        Matcher m = p.matcher(text);
        while (m.find()) {
            String num = m.group(1).replace(",", "");
            BigDecimal v = safeParse(num);
            if (v != null && v.signum() > 0) return v;
        }
        return null;
    }

    /** 兜底：找出所有两位小数的金额，取最大值（发票合计通常最大） */
    private BigDecimal pickMaxDecimal(String text) {
        Matcher m = P_DECIMAL.matcher(text);
        List<BigDecimal> candidates = new ArrayList<>();
        while (m.find()) {
            String num = (m.group(1).replace(",", "") + "." + m.group(2));
            BigDecimal v = safeParse(num);
            if (v != null && v.signum() > 0) candidates.add(v);
        }
        if (candidates.isEmpty()) return null;
        return candidates.stream().max(BigDecimal::compareTo).orElse(null);
    }

    private BigDecimal safeParse(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
