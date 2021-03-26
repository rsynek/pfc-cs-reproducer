/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.pfc.domain;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Task {

    private String id;
    private String name;
    private String state;

    private String description;
    private String referenceName;
    private String priority;

    private String processInstanceId;
    private String processId;
    private String rootProcessInstanceId;
    private String rootProcessId;

    private Set<String> potentialUsers = new HashSet<>();
    private Set<String> potentialGroups = new HashSet<>();
    private Set<String> adminUsers = new HashSet<>();
    private Set<String> adminGroups = new HashSet<>();
    private Set<String> excludedUsers = new HashSet<>();

    private ZonedDateTime started;
    private ZonedDateTime completed;
    private ZonedDateTime lastUpdate;

    private String endpoint;

    private Map<String, Object> inputData = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();

    Task() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
    }

    public void setRootProcessId(String rootProcessId) {
        this.rootProcessId = rootProcessId;
    }

    public void setStarted(ZonedDateTime started) {
        this.started = started;
    }

    public void setCompleted(ZonedDateTime completed) {
        this.completed = completed;
    }

    public void setLastUpdate(ZonedDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public static class Builder {

        private Task task = new Task();

        private Builder() {
        }

        public Task build() {
            return task;
        }

        public Builder id(String id) {
            task.setId(id);
            return this;
        }

        public Builder name(String name) {
            task.setName(name);
            return this;
        }

        public Builder state(String state) {
            task.setState(state);
            return this;
        }

        public Builder description(String description) {
            task.setDescription(description);
            return this;
        }

        public Builder referenceName(String referenceName) {
            task.setReferenceName(referenceName);
            return this;
        }

        public Builder priority(String priority) {
            task.setPriority(priority);
            return this;
        }

        public Builder processInstanceId(String processInstanceId) {
            task.setProcessInstanceId(processInstanceId);
            return this;
        }

        public Builder processId(String processId) {
            task.setProcessId(processId);
            return this;
        }

        public Builder rootProcessInstanceId(String rootProcessInstanceId) {
            task.setRootProcessInstanceId(rootProcessInstanceId);
            return this;
        }

        public Builder rootProcessId(String rootProcessId) {
            task.setRootProcessId(rootProcessId);
            return this;
        }

        public Builder started(ZonedDateTime started) {
            task.setStarted(started);
            return this;
        }

        public Builder completed(ZonedDateTime completed) {
            task.setCompleted(completed);
            return this;
        }

        public Builder lastUpdate(ZonedDateTime lastUpdate) {
            task.setLastUpdate(lastUpdate);
            return this;
        }

        public Builder endpoint(String endpoint) {
            task.setEndpoint(endpoint);
            return this;
        }
    }
}
