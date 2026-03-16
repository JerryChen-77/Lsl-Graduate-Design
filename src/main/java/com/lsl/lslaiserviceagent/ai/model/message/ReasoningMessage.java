package com.lsl.lslaiserviceagent.ai.model.message;

import dev.langchain4j.model.chat.response.PartialThinking;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ReasoningMessage extends StreamMessage {
    private String data;

    public ReasoningMessage(PartialThinking thinking) {
        super(StreamMessageTypeEnum.REASONING_MESSAGE.getValue());
        this.data = thinking.text();
    }
}
