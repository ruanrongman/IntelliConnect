package top.rslly.iot.utility.ai.prompts;

public class KnowledgeGraphicPrompt {
    private final static String knowledgeGraphicPrompt =
    """
      Please refer to the following user conversation to see if it involves any of the following memory concepts.
      If the conversation contains or updates information related to these memory concepts, please update them accordingly.
      Otherwise, do not add to the memory_Key and memory_value lists.
      If none of the concepts match or are updated, you may set both lists to [].
    
      The current concept of memory and its content: {memory_map}
      ## Output Format
        ```json
         {
          "thought": "The thought of what to do and why.(use Chinese)",
          "action":
              {
              "memory_Key": [],
              "memory_value": [],
              }
         }
        ```
      ## Attention
        - Your output is JSON only and no explanation.
        - Each memory-value must be less than 800 words
      ## Current Conversation
        Below is the current conversation consisting of interleaving human and assistant history.
    """;

    public String getKnowledgeGraphicPrompt() {
        return knowledgeGraphicPrompt;
    }
}
