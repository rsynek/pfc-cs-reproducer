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


import static org.example.pfc.domain.TaskAssignment.PREVIOUS_ELEMENT;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public abstract class ChainElement {

    protected String id;

    @InverseRelationShadowVariable(sourceVariableName = PREVIOUS_ELEMENT)
    protected TaskAssignment nextElement;

    protected ChainElement() {
    }

    protected ChainElement(String id) {
        this.id = id;
    }

    @PlanningId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TaskAssignment getNextElement() {
        return nextElement;
    }

    public void setNextElement(TaskAssignment nextElement) {
        this.nextElement = nextElement;
    }

    public abstract boolean isTaskAssignment();
}
