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
package top.rslly.iot.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15q.BgeSmallZhV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
  @Value("${rag.embedding_modelName}")
  private String embeddingModelName;
  @Value("${rag.knowledge_chat_embeddingStore_url}")
  private String knowledgeChatEmbeddingStoreUrl;
  @Value("${ai.siliconFlow-Key}")
  private String siliconFlowApiKey;

  @Bean
  public EmbeddingModel embeddingModel() {
    if (embeddingModelName.equals("bge-small-zh-v1.5-quantized")) {
      return new BgeSmallZhV15QuantizedEmbeddingModel();
    } else if (embeddingModelName.equals("silicon-qwen-Embedding-0.6B")) {
      return OpenAiEmbeddingModel.builder()
          .apiKey(siliconFlowApiKey)
          .baseUrl("https://api.siliconflow.cn/v1")
          .modelName("Qwen/Qwen3-Embedding-0.6B")
          .build();
    } else
      return null;
  }

  @Bean
  public EmbeddingStore<TextSegment> knowledgeChatEmbeddingStore() {
    return ChromaEmbeddingStore.builder()
        .baseUrl(knowledgeChatEmbeddingStoreUrl)
        .collectionName("knowledge_chat")
        .logRequests(true)
        .logResponses(true)
        .build();
  }
}
