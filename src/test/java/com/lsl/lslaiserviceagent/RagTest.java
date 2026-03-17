package com.lsl.lslaiserviceagent;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

public class RagTest {
    public static void main(String[] args) throws URISyntaxException {

            List<Document> documents = FileSystemDocumentLoader.loadDocuments("rag_documents");
            System.out.println(documents);

    }
}
