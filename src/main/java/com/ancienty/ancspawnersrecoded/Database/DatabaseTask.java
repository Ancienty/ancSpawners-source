package com.ancienty.ancspawnersrecoded.Database;

import javax.annotation.Nullable;

public class DatabaseTask {

    private String query;
    private Object[] parameters;
    public DatabaseTask(String query, Object[] parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String getQuery() {
        return query;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
