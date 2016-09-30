package org.zstack.header.volume;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListVolumeMsg extends APIListMessage {
    public APIListVolumeMsg() {
    }
    
    public APIListVolumeMsg(List<String> uuids) {
        super(uuids);
    }
}
