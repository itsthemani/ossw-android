package com.althink.android.ossw.service.ble;

/**
 * Created by krzysiek on 10/10/15.
 */
public interface ReadCharacteristicHandler {
    void handleValue(byte[] data);
}
