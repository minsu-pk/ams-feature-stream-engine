package com.kbank.ams.featurestreamengine.application.port.out;

import com.kbank.ams.featurestreamengine.domain.command.StoreJdbcCommand;

public interface StoreJdbcPort {
    int store(StoreJdbcCommand command);
}
