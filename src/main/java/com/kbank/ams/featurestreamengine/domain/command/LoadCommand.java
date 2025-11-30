package com.kbank.ams.featurestreamengine.domain.command;

public sealed interface LoadCommand permits LoadJdbcCommand, LoadOneJdbcCommand {
}
