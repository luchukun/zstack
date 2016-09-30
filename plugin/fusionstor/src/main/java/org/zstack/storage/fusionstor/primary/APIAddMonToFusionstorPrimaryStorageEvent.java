package org.zstack.storage.fusionstor.primary;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/6/2015.
 */
public class APIAddMonToFusionstorPrimaryStorageEvent extends APIEvent {
    private FusionstorPrimaryStorageInventory inventory;

    public APIAddMonToFusionstorPrimaryStorageEvent() {
    }

    public APIAddMonToFusionstorPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public FusionstorPrimaryStorageInventory getInventory() {
        return inventory;
    }

    public void setInventory(FusionstorPrimaryStorageInventory inventory) {
        this.inventory = inventory;
    }
}
