package com.ancienty.ancspawners.Database;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.ancienty.ancspawners.Database.Database.operations_queue;

public class DatabaseTask {
    private String query;
    private Object[] parameters;
    private String select_parameter;
    private boolean is_boolean;
    private CompletableFuture<Object> return_element;
    private final Object completionLock = new Object();

    public DatabaseTask(String query, Object[] parameters, @Nullable String select_parameter) {
        this.query = query;
        this.parameters = parameters;
        this.select_parameter = select_parameter;
        this.is_boolean = false;
    }

    public DatabaseTask(String query, Object[] parameters, @Nullable String select_parameter, boolean is_boolean) {
        this.query = query;
        this.parameters = parameters;
        this.select_parameter = select_parameter;
        this.is_boolean = is_boolean;
    }

    public boolean is_boolean() {
        return this.is_boolean;
    }

    public String getQuery() {
        return query;
    }

    public Object[] getParameters() {
        return parameters;
    }
    public String getSelectParameter() {return select_parameter;}

    public void setReturnElement(CompletableFuture<Object> result) {
        this.return_element = result;
        synchronized (completionLock) {
            completionLock.notify();
        }
    }

    public CompletableFuture<?> getReturnElement() {
        return this.return_element;
    }

    public CompletableFuture<List<String>> getReturnList() {
        synchronized (completionLock) {
            while (return_element == null) {
                try {
                    completionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return return_element.thenApply(value -> (List<String>) value);
    }

    public CompletableFuture<Integer> getReturnInteger() {
        synchronized (completionLock) {
            while (return_element == null) {
                try {
                    completionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return return_element.thenApply(value -> {
            if (value == null) {
                value = 0;
            } else {
                value = Integer.parseInt(String.valueOf(value));
            }
            return (Integer) value;
        });
    }

    public CompletableFuture<String> getReturnString() {
        synchronized (completionLock) {
            while (return_element == null) {
                try {
                    completionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return return_element.thenApply(value -> (String) value);
    }

    public CompletableFuture<Boolean> getReturnBoolean() {
        synchronized (completionLock) {
            while (return_element == null) {
                try {
                    completionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return return_element.thenApply(value -> (Boolean) value);
    }

    public CompletableFuture<Double> getReturnDouble() {
        synchronized (completionLock) {
            while (return_element == null) {
                try {
                    completionLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return return_element.thenApply(value -> (Double) value);
    }
}
