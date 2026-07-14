package com.declaration.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Excel → PDF 转换服务（LibreOffice headless）
 * <p>
 * 服务器需安装 LibreOffice：
 * <ul>
 *   <li>Linux:  {@code apt install libreoffice}</li>
 *   <li>Windows: 下载安装 LibreOffice，默认路径 C:\Program Files\LibreOffice</li>
 * </ul>
 * <p>
 * application.yml 配置：
 * <pre>
 * libreoffice:
 *   enabled: true
 *   path: libreoffice                          # Linux 默认
 *   # path: C:\\Program Files\\LibreOffice\\program\\soffice.exe   # Windows
 *   timeout: 120
 * </pre>
 */
@Slf4j
@Service
public class ExcelToPdfConverterService {

    @Value("${libreoffice.enabled:false}")
    private boolean enabled;

    /** LibreOffice 可执行文件路径（Linux: libreoffice / soffice；Windows: soffice.exe 完整路径） */
    @Value("${libreoffice.path:libreoffice}")
    private String libreOfficePath;

    /** 转换超时时间（秒） */
    @Value("${libreoffice.timeout:120}")
    private int timeoutSeconds;

    /**
     * 将 Excel 文件转换为 PDF（同名 .pdf 输出到同目录）
     *
     * @param xlsxFile xlsx 文件对象
     * @return 生成的 PDF 文件，转换失败返回 null
     */
    public File convertToPdf(File xlsxFile) {
        if (!enabled) {
            log.info("[PDF转换] LibreOffice 未启用，跳过 PDF 生成（配置 libreoffice.enabled=true）");
            return null;
        }
        if (!xlsxFile.exists()) {
            log.warn("[PDF转换] 源文件不存在: {}", xlsxFile.getAbsolutePath());
            return null;
        }

        File outDir = xlsxFile.getParentFile();
        if (outDir == null) outDir = new File(".");

        try {
            // LibreOffice headless 转换命令
            // 使用独立 UserInstallation 目录避免多进程锁冲突
            String[] cmd = {
                libreOfficePath,
                "--headless",
                "--norestore",
                "-env:UserInstallation=file:///tmp/libreoffice_user",
                "--convert-to", "pdf",
                "--outdir", outDir.getAbsolutePath(),
                xlsxFile.getAbsolutePath()
            };

            log.info("[PDF转换] 执行命令: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出（避免缓冲区满导致死锁）
            String output;
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                output = sb.toString();
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("[PDF转换] 超时（{}s），强制终止进程", timeoutSeconds);
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("[PDF转换] 进程退出码 {}，输出:\n{}", exitCode, output);
                return null;
            }

            // LibreOffice 输出文件名：把 .xlsx 换成 .pdf
            String baseName = xlsxFile.getName().replaceFirst("\\.[^.]+$", "");
            File pdfFile = new File(outDir, baseName + ".pdf");

            if (!pdfFile.exists()) {
                log.error("[PDF转换] PDF 文件未生成，预期路径: {}", pdfFile.getAbsolutePath());
                log.error("[PDF转换] LibreOffice 输出:\n{}", output);
                return null;
            }

            log.info("[PDF转换] 成功生成 PDF: {} ({}KB)", pdfFile.getAbsolutePath(), pdfFile.length() / 1024);
            return pdfFile;

        } catch (IOException e) {
            log.error("[PDF转换] IO 异常，请检查 LibreOffice 是否正确安装（path={}）: {}", libreOfficePath, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[PDF转换] 进程被中断", e);
            return null;
        }
    }

    /**
     * 检查 LibreOffice 是否可用
     */
    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder(libreOfficePath, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean ok = p.waitFor(10, TimeUnit.SECONDS);
            if (ok && p.exitValue() == 0) {
                String ver = new String(p.getInputStream().readAllBytes()).trim();
                log.info("[PDF转换] LibreOffice 可用: {}", ver);
                return true;
            }
        } catch (Exception e) {
            log.warn("[PDF转换] LibreOffice 不可用（path={}）: {}", libreOfficePath, e.getMessage());
        }
        return false;
    }
}
