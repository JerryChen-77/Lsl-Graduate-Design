package com.lsl.lslaiserviceagent.crawler.parser;

import com.lsl.lslaiserviceagent.model.enums.OperatorEnum;

public class ParserFactory {
    public static Parser getParser(OperatorEnum operator){
        switch (operator){
            case ChinaMobile -> {
                return new ChinaMobileParser();
            }
            case ChinaUnicom -> {
                return new ChinaUnicomParser();
            }
            case ChinaTelecom -> {
                return new ChinaTelecomParser();
            }
            default -> {
                return null;
            }
        }
    }
}
