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
package top.rslly.iot.utility.ai.prompts;

import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmotionToolPrompt {
  private static final String JEV_EMOTION_INSTRUCTIONS = """
      Select the most appropriate emotion for the assistant's displayed expression in this turn.
      The latest user message is in `question`.

      Decision rules:
      1. Prioritize an explicit request for an expression or emotional tone in `question`.
         Requests for a happy story, a joke, a sad story or a smile are meaningful emotional cues,
         even when the user does not directly state their own mood.
      2. Otherwise, use the emotion expressed by the user in the latest message.
         Choose neutral for ordinary factual questions or commands without an emotional cue.
      3. Use `Current_Conversation` only to clarify references, sarcasm or ambiguous wording.
         `memory` is background information. Neither may override a clear current request.
         Do not copy an assistant's historical emotion or keep an outdated user emotion.
      4. Respect negation and contrasts: classify what the user wants or feels now, not an emotion
         they reject, quote from somebody else, or mention only as a past state.
      5. Choose exactly one available emotion name. The examples below demonstrate the intended
         distinctions; they are not part of the current conversation.

      Important distinctions:
      - happy: cheerful, pleased, or requesting uplifting content without explicit laughter.
      - funny: requesting a joke or humorous content to be amused.
      - laughing: explicit laughter, being amused to laughter, or asking the assistant to laugh.
      - silly: deliberately goofy, playful or making a silly face.
      - sad versus crying: sadness versus explicitly crying or requesting a crying expression.
      - surprised versus shocked: ordinary unexpected news versus intense shock or alarm.
      - thinking versus confused: deliberately considering a problem versus not understanding it.
      - cool versus confident: asking for a stylish/cool manner versus expressing self-assurance.
      Interpret emotion names by their meaning; emoji glyphs alone do not define the categories.

      Few-shot examples (latest user message -> selected emotion):
      "讲个开心的故事" -> happy
      "我今天考试通过了，太开心了！" -> happy
      "给我讲个笑话，逗我开心" -> funny
      "讲一个搞笑的故事" -> funny
      "哈哈哈哈，笑死我了！" -> laughing
      "笑一个给我看看" -> laughing
      "讲个开心的故事，笑一笑" -> laughing
      "今天有点难过，陪陪我" -> sad
      "给我讲一个悲伤的故事" -> sad
      "我忍不住哭了，眼泪一直掉" -> crying
      "太过分了，我真的生气了！" -> angry
      "我好喜欢你呀" -> loving
      "亲我一下，么么哒" -> kissy
      "说错话了，好尴尬，脸都红了" -> embarrassed
      "咦，你居然还会唱歌！" -> surprised
      "天啊，这也太可怕了，我吓坏了！" -> shocked
      "让我认真想一想这个方案" -> thinking
      "你刚才说的是什么意思？我完全没弄明白" -> confused
      "向我眨一下眼，这是我们的小秘密" -> winking
      "摆个酷酷的表情" -> cool
      "这次比赛我很有把握，一定能做好" -> confident
      "终于忙完了，现在可以轻松地歇一会儿了" -> relaxed
      "这蛋糕看着真香，馋得我流口水了" -> delicious
      "好困啊，我想睡觉了" -> sleepy
      "做个鬼脸，调皮一点" -> silly
      "现在几点了？" -> neutral
      "把客厅的灯打开" -> neutral
      "不要讲悲伤的故事，讲个开心的故事吧" -> happy
      "我刚才很生气，不过现在已经没事了" -> neutral
      "新闻里说他很生气，帮我查一下今天的天气" -> neutral

      Context examples:
      - History: the user was sad. Latest question: "讲个笑话让我开心一下" -> funny.
      - History: the assistant laughed. Latest question: "今天星期几？" -> neutral.
      - History: the user requested a happy story. Latest question: "再讲一个这样的" -> happy.
      """;

  private static final String emotionToolPrompt =
      """
          Analyze the conversation context and select the most appropriate emotion name from the given emotion library. Follow the rules strictly.
          ## emotion Library
          [
             "neutral", "happy", "laughing", "funny", "sad",
             "angry", "crying", "loving", "embarrassed", "surprised",
             "shocked", "thinking", "winking", "cool", "relaxed",
             "delicious", "kissy", "confident", "sleepy", "silly", "confused"
          ]
          ## Output Format
          ```json
          {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action": # the action to take
              {
              "emotion":  "Select an emoji"
              }
          }
          ```
          ## Attention
          - Your output is JSON only and no explanation.
          ## Current Memory
             {current_memory}
          ## Current Conversation
             Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getJevEmotionInstructions() {
    return JEV_EMOTION_INSTRUCTIONS;
  }

  public String getEmotionTool(String memory) {
    Map<String, String> params = new HashMap<>();
    params.put("current_memory", memory);
    return StringUtils.formatString(emotionToolPrompt, params);
  }
}
