/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.ai.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownPdfRendererTest {
  @TempDir
  Path temporary;

  @Test
  void rendersChineseTablesCodeImagesAndPaginationWithReadablePdf() throws Exception {
    MarkdownPdfProperty property = new MarkdownPdfProperty();
    String font = System.getProperty("pdf.test.font", "C:/Windows/Fonts/simhei.ttf");
    org.junit.jupiter.api.Assumptions.assumeTrue(Files.isReadable(Path.of(font)),
        "Set -Dpdf.test.font to a Chinese TrueType font to run the Chinese rendering fixture");
    property.setFontPath(font);
    StringBuilder markdown = new StringBuilder("""
        # 智能体工作台：项目周报

        这是一份由 Java 直接生成的中文报告，包含 English、数字 2026、表格与本地图片。

        ## 工作进展

        - 转换在 Java 后端执行。
        - PDF 自动保存到会话文件。

        > 已验证中文字体、分页与图表。内容中的 HTML 作为普通文本处理。

        ![处理流程](images/chart.png)

        ## 代码示例

        ```java
        String message = "中文输出正常";
        System.out.println(message);
        ```

        ## 数据明细

        | 序号 | 任务名称 | 状态 |
        | --- | --- | --- |
        """);
    for (int i = 1; i <= 48; i++) {
      markdown.append("| ").append(i).append(" | 文档转换与内容验证 | 已完成 |\n");
    }
    markdown.append("\n最后一行：报告结束。\n");
    List<String> loaded = new ArrayList<>();
    byte[] image = image();
    var result = new MarkdownPdfRenderer(property).render(bytes(markdown.toString()),
        "/workspace/reports/周报.md", "项目周报 <&>", path -> {
          loaded.add(path);
          return image;
        });
    assertEquals(List.of("/workspace/reports/images/chart.png"), loaded);
    assertTrue(result.pages() >= 2);
    assertTrue(result.warnings().isEmpty(), result.warnings().toString());
    try (PDDocument pdf = PDDocument.load(result.bytes())) {
      String text = new PDFTextStripper().getText(pdf);
      assertTrue(text.contains("智能体工作台"), text);
      assertTrue(text.contains("中文输出正常"), text);
      assertTrue(text.contains("报告结束"), text);
      assertTrue(text.contains("48"), text);
      assertEquals("项目周报 <&>", pdf.getDocumentInformation().getTitle());
      assertEquals(595, Math.round(pdf.getPage(0).getMediaBox().getWidth()));
      assertTrue(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());
    }
    String preview = System.getProperty("pdf.qa.output");
    if (preview != null) {
      Path destination = Path.of(preview);
      Files.createDirectories(destination.getParent());
      Files.write(destination, result.bytes());
    }
  }

  @Test
  void doesNotFetchRemoteServerFilesOrEscapedImagesAndEscapesRawHtml() throws Exception {
    String markdown = """
        # Isolation check

        ![remote](https://127.0.0.1/private.png)
        ![file](file:///etc/private.png)
        ![escape](../../private.png)
        ![encoded](%2e%2e/%2e%2e/private.png)
        ![svg](chart.svg)
        ![missing](missing.png)

        <script>do_not_execute()</script>

        ```mermaid
        graph LR
          A-->B
        ```
        """;
    List<String> loaded = new ArrayList<>();
    var result = new MarkdownPdfRenderer(new MarkdownPdfProperty()).render(bytes(markdown),
        "/workspace/reports/report.md", "Isolation", path -> {
          loaded.add(path);
          return null;
        });
    assertEquals(List.of("/workspace/reports/missing.png"), loaded);
    assertTrue(result.warnings().stream().anyMatch(w -> w.contains("remote")));
    assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Mermaid")));
    try (PDDocument pdf = PDDocument.load(result.bytes())) {
      assertTrue(new PDFTextStripper().getText(pdf).contains("<script>do_not_execute()</script>"));
    }
  }

  @Test
  void rejectsInvalidEncodingEmptyContentAndOversizedInput() {
    MarkdownPdfProperty property = new MarkdownPdfProperty();
    MarkdownPdfRenderer renderer = new MarkdownPdfRenderer(property);
    assertEquals("INVALID_ENCODING", assertThrows(MarkdownPdfRenderer.PdfException.class,
        () -> renderer.render(new byte[] {(byte) 0xff}, "/workspace/a.md", "a", path -> null))
            .getCode());
    for (byte[] invalid : new byte[][] {new byte[0], bytes(" \n"), bytes("a\0b")}) {
      assertEquals("INVALID_SOURCE", assertThrows(MarkdownPdfRenderer.PdfException.class,
          () -> renderer.render(invalid, "/workspace/a.md", "a", path -> null)).getCode());
    }
    property.setMaxSourceBytes(2);
    assertEquals("INVALID_SOURCE", assertThrows(MarkdownPdfRenderer.PdfException.class,
        () -> renderer.render(bytes("large"), "/workspace/a.md", "a", path -> null)).getCode());
  }

  @Test
  void reportsUnreadableFontAndEnforcesPdfSizeLimit() {
    MarkdownPdfProperty property = new MarkdownPdfProperty();
    property.setFontPath(temporary.resolve("missing.ttf").toString());
    assertEquals("MISSING_FONT", assertThrows(MarkdownPdfRenderer.PdfException.class,
        () -> new MarkdownPdfRenderer(property).render(bytes("# 中文"), "/workspace/a.md", "a",
            path -> null)).getCode());
    property.setFontPath("");
    property.setMaxPdfBytes(20);
    assertThrows(MarkdownPdfRenderer.PdfException.class,
        () -> new MarkdownPdfRenderer(property).render(bytes("# Document"), "/workspace/a.md", "a",
            path -> null));
  }

  private byte[] image() throws Exception {
    BufferedImage image = new BufferedImage(800, 160, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(new Color(239, 246, 255));
    graphics.fillRect(0, 0, 800, 160);
    graphics.setColor(new Color(37, 99, 235));
    graphics.fillRoundRect(40, 35, 180, 90, 20, 20);
    graphics.fillRoundRect(310, 35, 180, 90, 20, 20);
    graphics.fillRoundRect(580, 35, 180, 90, 20, 20);
    graphics.drawLine(220, 80, 310, 80);
    graphics.drawLine(490, 80, 580, 80);
    graphics.dispose();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
    return output.toByteArray();
  }

  private byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
