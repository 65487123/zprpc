package com.lzp.protocol.zpproto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class AbstractDTO implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractDTO.class);

    public byte[] toBytes(){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(os);
            out.writeObject(this);
            return os.toByteArray();
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
        return null;
    }
}
