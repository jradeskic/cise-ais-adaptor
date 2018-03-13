package eu.cise.adaptor.tbs;

import dk.tbsalling.aismessages.AISInputStreamReader;
import eu.cise.adaptor.AISAdaptorException;
import eu.cise.adaptor.AISMessageConsumer;
import eu.cise.adaptor.AISSource;

import java.io.IOException;
import java.io.InputStream;

public class FileAISSource implements AISSource {

    private final AISInputStreamReader aisStream;

    public FileAISSource(String filename, AISMessageConsumer aisMessageConsumer) {
        InputStream inputStream = getClass().getResourceAsStream(filename);
        aisStream = new AISInputStreamReader(inputStream, aisMessageConsumer);
    }

    public void startConsuming() {
        try {
            aisStream.run();
        } catch (IOException e) {
            throw new AISAdaptorException(e);
        }
    }

}
