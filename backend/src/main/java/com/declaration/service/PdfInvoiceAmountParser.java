package com.declaration.service;

import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发票 PDF 解析服务。
 * 使用 RapidOCR (PaddleOCR v4) 直接对 PDF 页面做 OCR 识别，
 * 提取金额（价税合计）、发票号码、开票日期。
 */
@Slf4j
@Service
public class PdfInvoiceAmountParser {

    @Data
    public static class PdfParseResult {
        private BigDecimal amount;
        private boolean success;
        private String errorMsg;
        private String textSnippet;
        private String invoiceNo;
        private String invoiceDate;
    }

    @Value("${ocr.enabled:true}")
    private boolean ocrEnabled;

    private boolean ocrReady = false;
    private static final int MAX_PAGES = 3;
    private static final int OCR_DPI = 200;

    public boolean isOcrReady() { return ocrReady; }

    @PostConstruct
    public void init() {
        log.info("====================================================");
        log.info("[OCR] PdfInvoiceAmountParser 初始化开始, ocrEnabled={}", ocrEnabled);
        if (!ocrEnabled) {
            log.info("[OCR] OCR 已关闭 (ocr.enabled=false)，不会加载模型");
            log.info("====================================================");
            return;
        }
        try {
            long t0 = System.currentTimeMillis();
            log.info("[OCR] 正在加载 RapidOCR PaddleOCR v4 模型...");
            InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
            ocrReady = true;
            log.info("[OCR] RapidOCR (PaddleOCR v4) 初始化成功, 耗时={}ms", System.currentTimeMillis() - t0);
        } catch (Throwable e) {
            log.error("[OCR] RapidOCR 初始化失败: {}", e.getMessage(), e);
            ocrReady = false;
        }
        log.info("[OCR] ocrReady={}, ocrEnabled={}", ocrReady, ocrEnabled);
        log.info("====================================================");
    }

    // ---- 金额正则 ----
    private static final Pattern P_XIAOXIE = Pattern.compile(
            "(?:小写|\\(小写\\)|（小写）)[\\s\\S]{0,30}?[¥￥][\\s\\u00a0]{0,5}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern P_TOTAL_XIAOXIE = Pattern.compile(
            "价税合计[\\s\\S]{0,80}?[¥￥][\\s\\u00a0]{0,5}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern P_TOTAL_CN = Pattern.compile(
            "(?:合计金额|总计|总金额)[^\\d¥￥\\-]{0,20}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern P_CURRENCY = Pattern.compile(
            "(?:[¥￥]|CNY|USD|RMB)[\\s\\u00a0]{0,5}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern P_TOTAL_EN = Pattern.compile(
            "(?:Total\\s*Amount|Grand\\s*Total|Amount\\s*Due|Net\\s*Amount|Total)[^\\d\\-]{0,25}(-?[\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_DECIMAL = Pattern.compile(
            "(-?[\\d]{1,3}(?:,[\\d]{3})*)\\.([\\d]{2})(?![\\d])");

    // ---- 发票号码 ----
    private static final Pattern P_INVOICE_NO = Pattern.compile(
            "(?:发票号码|发票号|Invoice\\s*No\\.?)[^\\d]{0,10}(\\d{8,20})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    // ---- 开票日期 ----
    private static final Pattern P_DATE_CN = Pattern.compile(
            "(?:开票日期|开具日期)[^\\d]{0,10}(\\d{4})年(\\d{1,2})月(\\d{1,2})日",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern P_DATE_NUM = Pattern.compile(
            "(?:开票日期|开具日期|Invoice\\s*Date)[^\\d]{0,10}(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    /** 全电发票常见：开票日期：20260511（无分隔符 YYYYMMDD） */
    private static final Pattern P_DATE_COMPACT = Pattern.compile(
            "(?:开票日期|开具日期)[^\\d]{0,10}(\\d{4})(\\d{2})(\\d{2})(?!\\d)",
            Pattern.UNICODE_CHARACTER_CLASS);

    // ===================== 主入口 =====================

    public PdfParseResult parseAmount(InputStream input) {
        PdfParseResult result = new PdfParseResult();
        log.info("[OCR] ============ 收到解析请求 ============");
        log.info("[OCR] 开始解析发票 PDF, ocrReady={}, ocrEnabled={}", ocrReady, ocrEnabled);

        if (!ocrReady) {
            log.warn("[OCR] 引擎未就绪，跳过解析");
            result.setSuccess(false);
            result.setErrorMsg("OCR 服务未就绪，请检查配置");
            return result;
        }

        byte[] pdfBytes;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) != -1) bos.write(buf, 0, n);
            pdfBytes = bos.toByteArray();
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg("读取 PDF 流失败");
            return result;
        }

        // 直接 OCR 识别
        log.info("[OCR] PDF 读取成功, {} bytes, 开始 OCR 识别...", pdfBytes.length);
        long t0 = System.currentTimeMillis();
        String ocrText = ocrFromPdf(pdfBytes);
        long elapsed = System.currentTimeMillis() - t0;

        if (ocrText == null || ocrText.trim().isEmpty()) {
            log.warn("[OCR] 识别完成({}ms)，但未提取到任何文字", elapsed);
            result.setSuccess(false);
            result.setErrorMsg("OCR 未识别到任何文字，请确认 PDF 内容可读");
            return result;
        }

        log.info("[OCR] 识别完成({}ms)，文本长度={}", elapsed, ocrText.length());
        log.info("[OCR] 识别文本(前500): {}", ocrText.length() > 500 ? ocrText.substring(0, 500) : ocrText);
        result.setTextSnippet(ocrText.length() > 500 ? ocrText.substring(0, 500) : ocrText);

        // 从 OCR 文本中提取字段
        result.setAmount(extractAmount(ocrText));
        result.setInvoiceNo(extractInvoiceNo(ocrText));
        result.setInvoiceDate(extractInvoiceDate(ocrText));

        log.info("[OCR] 提取结果: amount={}, invoiceNo={}, invoiceDate={}",
                result.getAmount(), result.getInvoiceNo(), result.getInvoiceDate());

        if (result.getAmount() != null) {
            result.setSuccess(true);
        } else {
            result.setSuccess(false);
            result.setErrorMsg("OCR 已识别文字但未匹配到金额");
        }
        return result;
    }

    // ===================== RapidOCR =====================

    private String ocrFromPdf(byte[] pdfBytes) {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), MAX_PAGES);
            StringBuilder sb = new StringBuilder();
            InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);

            for (int i = 0; i < pages; i++) {
                log.info("[OCR] 渲染第{}页为图片(DPI={})...", i + 1, OCR_DPI);
                BufferedImage image = renderer.renderImageWithDPI(i, OCR_DPI);
                Path tmpFile = Files.createTempFile("ocr_page_", ".png");
                try {
                    ImageIO.write(image, "png", tmpFile.toFile());
                    log.info("[OCR] 第{}页图片已写入临时文件，开始识别...", i + 1);
                    OcrResult ocrResult = engine.runOcr(tmpFile.toString());
                    if (ocrResult != null && ocrResult.getStrRes() != null) {
                        String pageText = ocrResult.getStrRes().trim();
                        log.info("[OCR] 第{}页识别到{}个字符", i + 1, pageText.length());
                        sb.append(pageText).append("\n");
                    } else {
                        log.warn("[OCR] 第{}页未识别到文字", i + 1);
                    }
                } finally {
                    Files.deleteIfExists(tmpFile);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("RapidOCR 处理失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // ===================== 字段提取 =====================

    private BigDecimal extractAmount(String text) {
        BigDecimal amount = tryMatch(text, P_XIAOXIE);
        if (amount == null) amount = tryMatch(text, P_TOTAL_XIAOXIE);
        if (amount == null) amount = tryMatch(text, P_TOTAL_CN);
        if (amount == null) amount = tryMatch(text, P_CURRENCY);
        if (amount == null) amount = tryMatch(text, P_TOTAL_EN);
        if (amount == null) amount = pickMaxDecimal(text);
        return amount;
    }

    private String extractInvoiceNo(String text) {
        Matcher m = P_INVOICE_NO.matcher(text);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private String extractInvoiceDate(String text) {
        Matcher m = P_DATE_CN.matcher(text);
        if (m.find()) {
            return formatInvoiceDate(m.group(1), m.group(2), m.group(3));
        }
        Matcher m2 = P_DATE_NUM.matcher(text);
        if (m2.find()) {
            return formatInvoiceDate(m2.group(1), m2.group(2), m2.group(3));
        }
        Matcher m3 = P_DATE_COMPACT.matcher(text);
        if (m3.find()) {
            return formatInvoiceDate(m3.group(1), m3.group(2), m3.group(3));
        }
        return null;
    }

    private String formatInvoiceDate(String year, String month, String day) {
        int y = Integer.parseInt(year);
        int mo = Integer.parseInt(month);
        int d = Integer.parseInt(day);
        if (mo < 1 || mo > 12 || d < 1 || d > 31) {
            return null;
        }
        return String.format("%04d-%02d-%02d", y, mo, d);
    }

    // ===================== 工具方法 =====================

    private BigDecimal tryMatch(String text, Pattern p) {
        Matcher m = p.matcher(text);
        while (m.find()) {
            String num = m.group(1).replace(",", "");
            BigDecimal v = safeParse(num);
            if (v != null && v.signum() > 0) return v;
        }
        return null;
    }

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
        try { return new BigDecimal(s.trim()); }
        catch (Exception e) { return null; }
    }
}
