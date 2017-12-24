package ru.javaops.masterjava.service.mail.util;

import lombok.AllArgsConstructor;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.io.input.ProxyInputStream;
import ru.javaops.masterjava.service.mail.Attachment;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Attachments {
    public static Attachment getAttachment(String name, InputStream inputStream) {
        return new Attachment(name, new DataHandler(new InputStreamDataSource(inputStream)));
    }

    //    http://stackoverflow.com/questions/2830561/how-to-convert-an-inputstream-to-a-datahandler
    //    http://stackoverflow.com/a/10783565/548473
    @AllArgsConstructor
    private static class InputStreamDataSource implements DataSource {
        private InputStream inputStream;

        @Override
        public InputStream getInputStream() throws IOException {
            return new CloseShieldInputStreamWithReset(inputStream);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public String getName() {
            return "";
        }

    }

    private static class CloseShieldInputStreamWithReset extends ProxyInputStream {
        public CloseShieldInputStreamWithReset(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            if (this.in.markSupported())
                this.in.reset();
            this.in = new ClosedInputStream();
        }

    }
}
