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
package top.rslly.iot.utility.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

public class RagUtility {
  /**
   * 解析 PDF → 分段 → 存入
   */
  public static void ingestPdfToChroma(String pdfPath, String productId, String fileName,
      EmbeddingModel embeddingModel,
      EmbeddingStore<TextSegment> embeddingStore) {
    File pdfFile = new File(pdfPath);
    DocumentParser parser = new ApachePdfBoxDocumentParser();
    Document document = FileSystemDocumentLoader.loadDocument(pdfFile.getAbsolutePath(), parser);

    DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
    List<TextSegment> segments = splitter.split(document);

    for (TextSegment segment : segments) {
      Map<String, String> metadataMap = new HashMap<>();
      metadataMap.put("productId", productId);
      metadataMap.put("fileName", fileName);
      Metadata metadata = Metadata.from(metadataMap);
      TextSegment segmentWithMetadata = TextSegment.from(segment.text(), metadata);
      Embedding embedding = embeddingModel.embed(segmentWithMetadata).content();
      embeddingStore.add(embedding, segmentWithMetadata);
    }
  }

  /**
   * 按 productId 过滤检索
   */
  public static EmbeddingSearchResult<TextSegment> searchByProductId(
      EmbeddingStore<TextSegment> store, EmbeddingModel embeddingModel,
      String query, String productId) {
    Embedding queryEmbedding = embeddingModel.embed(query).content();
    Filter filterByUser = metadataKey("productId").isEqualTo(productId);

    var searchRequest = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .filter(filterByUser)
        .maxResults(5)
        .minScore(0.6)
        .build();

    return store.search(searchRequest);
  }

  /**
   * 清除特定 productId 和 fileName 的嵌入数据
   */
  public static void deleteAllByProductIdAndFilename(EmbeddingStore<TextSegment> store,
      String productId,
      String fileName) {
    // 构建复合过滤条件：同时匹配 userId 和 fileName
    Filter filter = metadataKey("productId").isEqualTo(productId)
        .and(metadataKey("fileName").isEqualTo(fileName));

    // 执行批量删除
    store.removeAll(filter);
  }
}
