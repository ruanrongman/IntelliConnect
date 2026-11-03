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

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Image;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/**
 * Pure Java renderer. Untrusted Markdown can fetch only bounded images through its supplied loader.
 */
@Component
@RequiredArgsConstructor
public class MarkdownPdfRenderer {
  private final MarkdownPdfProperty property;
  private static final List<org.commonmark.Extension> EXTENSIONS =
      List.of(TablesExtension.create());
  private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
  private static final HtmlRenderer HTML = HtmlRenderer.builder().extensions(EXTENSIONS)
      .escapeHtml(true).sanitizeUrls(true).build();
  private static final String CSS =
      """
          @page { size: A4; margin: 20mm 17mm;
            @bottom-center { content: counter(page) " / " counter(pages); font-size: 9pt; color: #64748b; }
          }
          body { font-family: document-font, sans-serif; font-size: 10pt; line-height: 1.65; color: #1e293b;
            word-wrap: break-word; }
          h1, h2, h3, h4, h5, h6 { line-height: 1.35; page-break-after: avoid; color: #0f172a; }
          h1 { font-size: 23pt; margin: 0 0 18pt; }
          h2 { font-size: 16pt; margin: 20pt 0 10pt; border-bottom: 1px solid #e2e8f0; padding-bottom: 5pt; }
          h3 { font-size: 12pt; margin: 14pt 0 7pt; }
          p, ul, ol { margin: 7pt 0; orphans: 3; widows: 3; }
          ul, ol { padding-left: 20pt; }
          a { color: #2563eb; text-decoration: none; word-wrap: break-word; }
          blockquote { margin: 10pt 0; padding: 2pt 12pt; border-left: 3px solid #94a3b8; color: #475569; }
          pre, code { font-family: document-font, monospace; font-size: 8.5pt; }
          code { background-color: #f1f5f9; }
          pre { background-color: #f1f5f9; padding: 10pt; white-space: pre-wrap; word-wrap: break-word; }
          table { border-collapse: collapse; width: 100%; table-layout: fixed; margin: 12pt 0; font-size: 9pt;
            -fs-table-paginate: paginate; }
          thead { display: table-header-group; }
          tr { page-break-inside: avoid; }
          th, td { border: 1px solid #cbd5e1; padding: 6pt 7pt; vertical-align: top; word-wrap: break-word; }
          th { background-color: #f1f5f9; }
          img { max-width: 100%; max-height: 225mm; }
          hr { border: 0; border-top: 1px solid #cbd5e1; margin: 14pt 0; }
          """;

  public Rendered render(byte[] source, String sourcePath, String title,
      Function<String, byte[]> imageLoader) {
    if (source == null || source.length == 0 || source.length > property.getMaxSourceBytes()) {
      throw new PdfException("INVALID_SOURCE",
          "Markdown source is empty or exceeds the size limit.");
    }
    try {
      String text = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(source)).toString().replaceFirst("^\\uFEFF", "");
      if (text.isBlank() || text.indexOf('\0') >= 0) {
        throw new PdfException("INVALID_SOURCE", "Markdown must contain non-empty UTF-8 text.");
      }
      List<String> warnings = new ArrayList<>();
      Map<String, byte[]> images = new HashMap<>();
      long[] totalImageBytes = {0};
      var node = PARSER.parse(text);
      node.accept(new AbstractVisitor() {
        @Override
        public void visit(FencedCodeBlock block) {
          if (block.getInfo().trim().equalsIgnoreCase("mermaid")) {
            warn(warnings, "Mermaid is displayed as code; render diagrams to local images first.");
          }
        }

        @Override
        public void visit(Image image) {
          String path = imagePath(sourcePath, image.getDestination());
          if (path == null) {
            warn(warnings,
                "Skipped remote, unsupported or unsafe image: " + image.getDestination());
            image.setDestination("blocked:/image");
            return;
          }
          if (!images.containsKey(path)) {
            byte[] data = null;
            if (images.size() >= 32 || totalImageBytes[0] >= 30L * 1024 * 1024) {
              warn(warnings, "Image count or total size limit exceeded: " + path);
              image.setDestination("blocked:/image");
              return;
            }
            try {
              data = imageLoader.apply(path);
              if (data != null && (data.length == 0 || data.length > property.getMaxImageBytes()
                  || data.length > 30L * 1024 * 1024 - totalImageBytes[0])) {
                data = null;
              }
            } catch (Exception e) {
              warn(warnings, "Unable to read image from sandbox: " + path);
            }
            if (data != null)
              totalImageBytes[0] += data.length;
            images.put(path, data);
          }
          if (images.get(path) == null) {
            warn(warnings, "Image unavailable or size/count limit exceeded: " + path);
            image.setDestination("blocked:/image");
          } else {
            image.setDestination(encodePath(path));
          }
        }
      });
      String document = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
          + "<meta charset=\"utf-8\"/><title>" + escape(title) + "</title><style>" + CSS
          + "</style></head><body>" + HTML.render(node) + "</body></html>";
      LimitedOutput output = new LimitedOutput(property.getMaxPdfBytes());
      try (PDDocument pdf = new PDDocument()) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.usePDDocument(pdf);
        Path fontPath = resolveFont();
        if (fontPath != null) {
          try (InputStream stream = Files.newInputStream(fontPath)) {
            PDType0Font font = PDType0Font.load(pdf, stream, true);
            boolean unsupported = text.codePoints().filter(MarkdownPdfRenderer::isCjk)
                .anyMatch(codePoint -> !hasGlyph(font, codePoint));
            if (unsupported) {
              throw new PdfException("MISSING_GLYPHS",
                  "The backend PDF font cannot display all Chinese/CJK characters; configure ai.pdf.font-path.");
            }
            builder.useFont(new PDFontSupplier(font), "document-font", 400, FontStyle.NORMAL, true);
            builder.useFont(new PDFontSupplier(font), "document-font", 700, FontStyle.NORMAL, true);
          } catch (PdfException e) {
            throw e;
          } catch (Exception e) {
            throw new PdfException("INVALID_FONT",
                "Cannot load the backend PDF font. Set ai.pdf.font-path to a readable TrueType .ttf font supporting Chinese.");
          }
        } else if (text.codePoints().anyMatch(MarkdownPdfRenderer::isCjk)) {
          throw new PdfException("MISSING_FONT",
              "Chinese PDF output requires a font on the Java backend. Configure ai.pdf.font-path to a TrueType .ttf font; sandbox image fonts are separate.");
        }
        builder.useUriResolver((base, uri) -> uri != null && uri.startsWith("/workspace/")
            ? "sandbox:" + uri
            : null);
        builder.useExternalResourceAccessControl(
            (uri, type) -> uri != null && uri.startsWith("sandbox:"),
            ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
        builder.useProtocolsStreamImplementation(uri -> {
          byte[] data = images.get(URI.create(uri).getPath());
          return new FSStream() {
            @Override
            public InputStream getStream() {
              return data == null ? null : new ByteArrayInputStream(data);
            }

            @Override
            public Reader getReader() {
              return null;
            }
          };
        }, "sandbox");
        builder.withDiagnosticConsumer(diagnostic -> {
          if (diagnostic.getLevel().intValue() >= java.util.logging.Level.WARNING.intValue()) {
            warn(warnings, diagnostic.getFormattedMessage());
          }
        });
        builder.withHtmlContent(document, "sandbox:/workspace/").toStream(output).useFastMode()
            .run();
      }
      byte[] bytes = output.toByteArray();
      try (PDDocument verified = PDDocument.load(bytes)) {
        int pages = verified.getNumberOfPages();
        if (pages == 0 || verified.isEncrypted()) {
          throw new PdfException("INVALID_PDF", "Generated PDF has no readable pages.");
        }
        new PDFTextStripper().getText(verified);
        return new Rendered(bytes, pages, List.copyOf(warnings));
      }
    } catch (PdfException e) {
      throw e;
    } catch (java.nio.charset.CharacterCodingException e) {
      throw new PdfException("INVALID_ENCODING", "Markdown source must use UTF-8 encoding.");
    } catch (Exception e) {
      throw new PdfException("CONVERSION_FAILED", "Java PDF rendering failed: " + e.getMessage());
    }
  }

  private Path resolveFont() {
    if (property.getFontPath() != null && !property.getFontPath().isBlank()) {
      Path configured = Path.of(property.getFontPath());
      if (!Files.isRegularFile(configured) || !Files.isReadable(configured)) {
        throw new PdfException("MISSING_FONT",
            "The configured ai.pdf.font-path is not readable on the Java backend.");
      }
      return configured;
    }
    for (String candidate : List.of("/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf",
        "/usr/share/fonts/truetype/noto/NotoSansSC-VF.ttf",
        "/usr/local/share/fonts/NotoSansSC-Regular.ttf",
        "C:/Windows/Fonts/simhei.ttf")) {
      Path path = Path.of(candidate);
      if (Files.isRegularFile(path) && Files.isReadable(path))
        return path;
    }
    return null;
  }

  private static boolean hasGlyph(PDType0Font font, int codePoint) {
    try {
      // PDType0Font.hasGlyph takes a PDF character code, not a Unicode code point.
      font.encode(new String(Character.toChars(codePoint)));
      return true;
    } catch (IOException | IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isCjk(int codePoint) {
    var script = Character.UnicodeScript.of(codePoint);
    return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
        || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL;
  }

  private String imagePath(String sourcePath, String destination) {
    try {
      URI uri = new URI(destination);
      if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getPath() == null
          || uri.getPath().contains("\\"))
        return null;
      String path = uri.getPath();
      path = path.startsWith("/") ? path
          : sourcePath.substring(0, sourcePath.lastIndexOf('/') + 1) + path;
      // POSIX normalization without depending on the backend OS.
      path = new URI(null, null, path, null).normalize().getPath();
      if (!path.startsWith("/workspace/") || path.contains("/../")
          || !path.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpg|jpeg|gif|bmp)$"))
        return null;
      return path;
    } catch (Exception e) {
      return null;
    }
  }

  private static String encodePath(String path) {
    try {
      return new URI(null, null, path, null).toASCIIString();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid image path", e);
    }
  }

  private static void warn(List<String> warnings, String message) {
    String bounded = message.substring(0, Math.min(message.length(), 500));
    if (warnings.size() < 30 && !warnings.contains(bounded))
      warnings.add(bounded);
  }

  private String escape(String value) {
    return value == null ? ""
        : value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
  }

  public record Rendered(byte[] bytes, int pages, List<String> warnings) {}

  public static class PdfException extends IllegalArgumentException {
    private final String code;

    public PdfException(String code, String message) {
      super(message);
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  private static class LimitedOutput extends ByteArrayOutputStream {
    private final int maximum;

    private LimitedOutput(int maximum) {
      this.maximum = maximum;
    }

    @Override
    public synchronized void write(byte[] data, int offset, int length) {
      if (length > maximum - count)
        throw new PdfException("PDF_TOO_LARGE", "PDF exceeds the output size limit.");
      super.write(data, offset, length);
    }

    @Override
    public synchronized void write(int value) {
      if (count >= maximum)
        throw new PdfException("PDF_TOO_LARGE", "PDF exceeds the output size limit.");
      super.write(value);
    }
  }
}
