package com.divya.linkedinclone.service;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ResumeParserService {

    public String parseResume(Path filePath) throws IOException, TikaException, SAXException {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1); // -1 for no write limit
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream stream = Files.newInputStream(filePath)) {
            parser.parse(stream, handler, metadata, context);
            return handler.toString();
        }
    }
}