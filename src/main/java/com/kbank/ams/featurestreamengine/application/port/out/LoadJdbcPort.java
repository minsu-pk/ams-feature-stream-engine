package com.kbank.ams.featurestreamengine.application.port.out;

import com.kbank.ams.featurestreamengine.domain.command.LoadOneJdbcCommand;
import java.util.Map;

public interface LoadJdbcPort {
    Map<String,Object> loadOne(LoadOneJdbcCommand command);
    Boolean loadBool(LoadOneJdbcCommand command);
}
