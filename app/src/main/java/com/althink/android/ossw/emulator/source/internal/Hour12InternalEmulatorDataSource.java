package com.althink.android.ossw.emulator.source.internal;

import com.althink.android.ossw.emulator.renderer.EmulatorExecutionContext;
import com.althink.android.ossw.emulator.source.EmulatorDataSource;
import com.althink.android.ossw.watchsets.DataSourceType;

import java.util.Calendar;

/**
 * Created by krzysiek on 14/06/15.
 */
public class Hour12InternalEmulatorDataSource implements EmulatorDataSource {

    @Override
    public Object getData(DataSourceType type, int range, EmulatorExecutionContext ctx) {
        if(!DataSourceType.NUMBER.equals(type)) {
            return null;
        }
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 12;
        return h == 0 ? 12 : h;
    }
}
