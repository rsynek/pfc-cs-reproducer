package org.example.pfc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.pfc.domain.ChainElement;
import org.example.pfc.domain.Task;
import org.example.pfc.domain.TaskAssigningSolution;
import org.example.pfc.domain.TaskAssignment;
import org.example.pfc.solver.AddTaskProblemFactChange;
import org.example.pfc.solver.TaskAssigningConstraintProvider;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProblemFactChangesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemFactChangesTest.class);
    private static final String DATA_SET = "24tasks-8users.xml";

    @Test
    void testAddingProblemFactChanges() {
        TaskAssigningSolution solution = readTaskAssigningSolution(DATA_SET);
        String taskId = "2000"; //No such task in the data set.
        TaskAssignment taskAssignment = new TaskAssignment(Task.newBuilder().id(taskId).build());

        final Semaphore allChangesWereProduced = new Semaphore(0);

        //prepare the list of changes to program
        List<AddTaskProblemFactChange> programmedChanges = Collections.singletonList(new AddTaskProblemFactChange(taskAssignment));
        AtomicInteger scheduledChanges = new AtomicInteger(0);
        int totalProgrammedChanges = programmedChanges.size();

        Solver<TaskAssigningSolution> solver = createSolver();
        solver.addEventListener(event -> {
            if (event.isEveryProblemFactChangeProcessed()) {
                solver.terminateEarly();
                allChangesWereProduced.release();
            }
        });

        CompletableFuture.runAsync(() -> {
            for (AddTaskProblemFactChange programmedChange : programmedChanges) {
                solver.addProblemFactChange(scoreDirector -> {
                    programmedChange.doChange(scoreDirector);
                    scheduledChanges.incrementAndGet();
                });
            }
            try {
                //wait until the solver listener has processed all the changes.
                allChangesWereProduced.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("Thread was interrupted while waiting on a semaphore.", e);
            }
        });

        solver.solve(solution);
        assertThat(scheduledChanges.get()).isEqualTo(totalProgrammedChanges);
    }

    private TaskAssigningSolution readTaskAssigningSolution(String resource) {
        File resourceFile = Paths.get(getClass().getResource(resource).getPath()).toFile();
        XStreamSolutionFileIO<TaskAssigningSolution> solutionFileIO = new XStreamSolutionFileIO<>(TaskAssigningSolution.class);
        return solutionFileIO.read(resourceFile);
    }

    private SolverConfig createBaseConfig() {
        SolverConfig config = new SolverConfig();
        config.setSolutionClass(TaskAssigningSolution.class);
        config.setEntityClassList(Arrays.asList(ChainElement.class, TaskAssignment.class));
        config.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig().withConstraintProviderClass(TaskAssigningConstraintProvider.class));
        return config;
    }

    private Solver<TaskAssigningSolution> createSolver() {
        SolverConfig config = createBaseConfig();
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.create(config);
        return solverFactory.buildSolver();
    }
}
