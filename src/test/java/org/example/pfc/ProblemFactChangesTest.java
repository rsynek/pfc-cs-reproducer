package org.example.pfc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.pfc.domain.TaskAssignment.PREVIOUS_ELEMENT;

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
import org.example.pfc.domain.User;
import org.example.pfc.solver.AddTaskProblemFactChange;
import org.example.pfc.solver.TaskAssigningConstraintProvider;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.api.solver.ProblemFactChange;
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
    private static final AtomicInteger changeIds = new AtomicInteger(1);

    protected static int nextChangeId() {
        return changeIds.getAndIncrement();
    }

    @Test
    void testAddingProblemFactChanges() {
        TaskAssigningSolution solution = readTaskAssigningSolution(DATA_SET);
        String taskId = "20"; //randomly selected task.
        TaskAssignment taskAssignment = new TaskAssignment(Task.newBuilder().id(taskId).build());

        List<ProgrammedProblemFactChange> changes = Collections.singletonList(new ProgrammedProblemFactChange<>(new AddTaskProblemFactChange(taskAssignment)));

        Solver<TaskAssigningSolution> solver = createDaemonSolver();

        final TaskAssigningSolution[] initialSolution = {null};
        final AtomicInteger lastExecutedChangeId = new AtomicInteger(-1);

        final Semaphore programNextChange = new Semaphore(0);
        final Semaphore allChangesWereProduced = new Semaphore(0);

        //prepare the list of changes to program
        List<ProgrammedProblemFactChange> programmedChanges = new ArrayList<>(changes);
        List<ProgrammedProblemFactChange> scheduledChanges = new ArrayList<>();

        int totalProgrammedChanges = programmedChanges.size();
        int[] pendingChanges = {programmedChanges.size()};

        solver.addEventListener(event -> {
            if (initialSolution[0] == null) {
                //store the first produced solution for knowing how things looked like at the very beginning.
                initialSolution[0] = event.getNewBestSolution();
                //let the problem fact changes start being produced.
                programNextChange.release();
            } else if (event.isEveryProblemFactChangeProcessed() && !scheduledChanges.isEmpty()) {
                ProgrammedProblemFactChange programmedChange = scheduledChanges.get(scheduledChanges.size() - 1);
                if (lastExecutedChangeId.compareAndSet(programmedChange.getId(), -1)) {
                    programmedChange.setSolutionAfterChange(event.getNewBestSolution());
                    if (pendingChanges[0] > 0) {
                        //let the Programmed changes producer produce next change
                        programNextChange.release();
                    } else {
                        solver.terminateEarly();
                        allChangesWereProduced.release();
                    }
                }
            }
        });

        //Programmed changes producer Thread.
        CompletableFuture.runAsync(() -> {
            boolean hasMoreChanges = true;
            while (hasMoreChanges) {
                try {
                    //wait until next problem fact change can be added to the solver.
                    //by construction the lock is only released when no problem fact change is in progress.
                    programNextChange.acquire();
                    ProgrammedProblemFactChange programmedChange = programmedChanges.remove(0);
                    hasMoreChanges = !programmedChanges.isEmpty();
                    pendingChanges[0] = programmedChanges.size();
                    scheduledChanges.add(programmedChange);
                    solver.addProblemFactChange(scoreDirector -> {
                        lastExecutedChangeId.set(programmedChange.getId());
                        programmedChange.getChange().doChange(scoreDirector);
                    });
                } catch (InterruptedException e) {
                    LOGGER.error("It looks like the test Future was interrupted.", e);
                }
            }
            try {
                //wait until the solver listener has processed all the changes.
                allChangesWereProduced.acquire();
            } catch (InterruptedException e) {
                LOGGER.error("It looks like the test Future was interrupted while waiting to finish.", e);
            }
        });

        solver.solve(solution);

        assertThat(programmedChanges.isEmpty()).isTrue();
        assertThat(scheduledChanges.size()).isEqualTo(totalProgrammedChanges);
        assertThat(pendingChanges[0]).isZero();
    }

    protected TaskAssigningSolution readTaskAssigningSolution(String resource) {
        File resourceFile = Paths.get(getClass().getResource(resource).getPath()).toFile();
        XStreamSolutionFileIO<TaskAssigningSolution> solutionFileIO = new XStreamSolutionFileIO<>(TaskAssigningSolution.class);
        return solutionFileIO.read(resourceFile);
    }

    protected SolverConfig createBaseConfig() {
        SolverConfig config = new SolverConfig();
        config.setSolutionClass(TaskAssigningSolution.class);
        config.setEntityClassList(Arrays.asList(ChainElement.class, TaskAssignment.class));
        config.setScoreDirectorFactoryConfig(new ScoreDirectorFactoryConfig().withConstraintProviderClass(TaskAssigningConstraintProvider.class));
        return config;
    }

    protected Solver<TaskAssigningSolution> createDaemonSolver() {
        SolverConfig config = createBaseConfig();
        config.setDaemon(true);
        SolverFactory<TaskAssigningSolution> solverFactory = SolverFactory.create(config);
        return solverFactory.buildSolver();
    }

    private static class AddTaskPFC implements ProblemFactChange<TaskAssigningSolution> {
        private TaskAssignment taskAssignment;
        private User user;
        private boolean addIfNotExists = false;

        public AddTaskPFC(TaskAssignment taskAssignment, User user) {
            this.taskAssignment = taskAssignment;
            this.user = user;
        }

        public AddTaskPFC(TaskAssignment taskAssignment, User user, boolean addIfNotExists) {
            this.taskAssignment = taskAssignment;
            this.user = user;
            this.addIfNotExists = addIfNotExists;
        }

        public TaskAssignment getTaskAssignment() {
            return taskAssignment;
        }

        public User getUser() {
            return user;
        }

        @Override
        public void doChange(ScoreDirector<TaskAssigningSolution> scoreDirector) {
            User workingUser = lookupOrAddWorkingUser(user, scoreDirector, addIfNotExists);
            TaskAssignment workingTaskAssignment = lookupOrPrepareTaskAssignment(taskAssignment, scoreDirector);
            ChainElement insertPosition = findInsertPosition(workingUser);
            TaskAssignment insertPositionNextTask = insertPosition.getNextElement();

            if (taskAssignment == workingTaskAssignment) {
                processNewTaskAssignment(workingTaskAssignment, insertPosition, insertPositionNextTask, scoreDirector);
            } else if (insertPosition != workingTaskAssignment) {
                // in cases where insertPosition == workingTaskAssignment there's nothing to do, since the workingTaskAssignment
                // is already pinned and belongs to user. (see findInsertPosition)
                processExistingTaskAssignment(workingTaskAssignment, insertPosition, insertPositionNextTask, scoreDirector);
            }
        }

        private void processNewTaskAssignment(TaskAssignment newTaskAssignment,
                                              ChainElement insertPosition,
                                              TaskAssignment insertPositionNextTask,
                                              ScoreDirector<TaskAssigningSolution> scoreDirector) {

            TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
            newTaskAssignment.setPreviousElement(insertPosition);
            scoreDirector.beforeEntityAdded(newTaskAssignment);
            // Planning entity lists are already cloned by the SolutionCloner, no need to clone.
            solution.getTaskAssignmentList().add(newTaskAssignment);
            scoreDirector.afterEntityAdded(newTaskAssignment);

            setPreviousElementIfApply(insertPositionNextTask, newTaskAssignment, scoreDirector);
            setPinned(newTaskAssignment, scoreDirector);
            scoreDirector.triggerVariableListeners();
        }

        private void processExistingTaskAssignment(TaskAssignment existingTaskAssignment,
                                                   ChainElement insertPosition,
                                                   TaskAssignment insertPositionNextTask,
                                                   ScoreDirector<TaskAssigningSolution> scoreDirector) {
            if (insertPosition.getNextElement() != existingTaskAssignment) {
                // relocate the existingTaskAssignment at the desired position
                ChainElement previousElement = existingTaskAssignment.getPreviousElement();
                if (previousElement != null) {
                    // unlink from the current chain position.
                    unlinkTaskAssignment(existingTaskAssignment, previousElement, scoreDirector);
                }
                scoreDirector.beforeVariableChanged(existingTaskAssignment, PREVIOUS_ELEMENT);
                existingTaskAssignment.setPreviousElement(insertPosition);
                scoreDirector.afterVariableChanged(existingTaskAssignment, PREVIOUS_ELEMENT);
                setPreviousElementIfApply(insertPositionNextTask, existingTaskAssignment, scoreDirector);
            }
            setPinned(existingTaskAssignment, scoreDirector);
            scoreDirector.triggerVariableListeners();
        }

        private static User lookupOrAddWorkingUser(User user, ScoreDirector<TaskAssigningSolution> scoreDirector, boolean addIfNotExists) {
            TaskAssigningSolution solution = scoreDirector.getWorkingSolution();
            User workingUser = scoreDirector.lookUpWorkingObjectOrReturnNull(user);
            if (workingUser == null) {
                if (!addIfNotExists) {
                    throw new IllegalStateException(String.format("Expected user: %s was not found in current working solution", user));
                } else {
                    // Shallow clone the user list so only workingSolution is affected, not bestSolution
                    solution.setUserList(new ArrayList<>(solution.getUserList()));
                    // Ensure that the nextElement value calculated by OptaPlanner doesn't have any out-side manually
                    // assigned value.
                    user.setNextElement(null);
                    scoreDirector.beforeProblemFactAdded(user);
                    solution.getUserList().add(user);
                    scoreDirector.afterProblemFactAdded(user);
                    workingUser = user;
                }
            }
            return workingUser;
        }

        private static TaskAssignment lookupOrPrepareTaskAssignment(TaskAssignment taskAssignment,
                                                                    ScoreDirector<TaskAssigningSolution> scoreDirector) {
            TaskAssignment workingTaskAssignment = scoreDirector.lookUpWorkingObjectOrReturnNull(taskAssignment);
            if (workingTaskAssignment != null) {
                return workingTaskAssignment;
            } else {
                // The task assignment will be created by this PFC.
                // Ensure that the task assignment to be added doesn't have any out-side manually assigned values for the
                // values that are calculated by OptaPlanner
                taskAssignment.setPreviousElement(null);
                taskAssignment.setUser(null);
                taskAssignment.setPinned(false);
                taskAssignment.setNextElement(null);
                taskAssignment.setStartTimeInMinutes(null);
                taskAssignment.setEndTimeInMinutes(null);
                return taskAssignment;
            }
        }

        /**
         * Find the first available "position" where a taskAssignment can be added in the tasks chain for a given user.
         * <p>
         * For a chain like:
         * <p>
         * U -> T1 -> T2 -> T3 -> T4 -> null
         * <p>
         * if e.g. T3 is returned, a new taskAssignment Tn will be later added in the following position.
         * <p>
         * U -> T1 -> T2 -> T3 -> Tn -> T4 -> null
         * Given that we are using a chained structure, to pin a task assignment Tn to a given user, we must be sure that
         * all the previous tasks in the chain are pinned to the same user. For keeping the structure consistency a task
         * assignment Tn is inserted after the last pinned element in the chain. In the example above we have that existing
         * tasks assignments T1, T2 and T3 are pinned.
         *
         * @param user the for adding a taskAssignment to.
         * @return the proper ChainElement object were a taskAssignment can be added. This method will never return null.
         */
        private static ChainElement findInsertPosition(User user) {
            ChainElement result = user;
            TaskAssignment nextTaskAssignment = user.getNextElement();
            while (nextTaskAssignment != null && nextTaskAssignment.isPinned()) {
                result = nextTaskAssignment;
                nextTaskAssignment = nextTaskAssignment.getNextElement();
            }
            return result;
        }

        private static void setPreviousElementIfApply(TaskAssignment insertPositionNextTask,
                                                      TaskAssignment previousElement,
                                                      ScoreDirector<TaskAssigningSolution> scoreDirector) {
            if (insertPositionNextTask != null) {
                scoreDirector.beforeVariableChanged(insertPositionNextTask, PREVIOUS_ELEMENT);
                insertPositionNextTask.setPreviousElement(previousElement);
                scoreDirector.afterVariableChanged(insertPositionNextTask, PREVIOUS_ELEMENT);
            }
        }

        private static void setPinned(TaskAssignment taskAssignment, ScoreDirector<TaskAssigningSolution> scoreDirector) {
            scoreDirector.beforeProblemPropertyChanged(taskAssignment);
            taskAssignment.setPinned(true);
            scoreDirector.afterProblemPropertyChanged(taskAssignment);
        }

        private static void unlinkTaskAssignment(TaskAssignment taskAssignment, ChainElement previousElement, ScoreDirector<TaskAssigningSolution> scoreDirector) {
            TaskAssignment nextTaskAssignment = taskAssignment.getNextElement();
            if (nextTaskAssignment != null) {
                scoreDirector.beforeVariableChanged(nextTaskAssignment, PREVIOUS_ELEMENT);
                nextTaskAssignment.setPreviousElement(previousElement);
                scoreDirector.afterVariableChanged(nextTaskAssignment, PREVIOUS_ELEMENT);
            }
        }
    }

    private static class ProgrammedProblemFactChange<C extends ProblemFactChange<TaskAssigningSolution>> {

        int id;

        private TaskAssigningSolution solutionAfterChange;

        private C change;

        public ProgrammedProblemFactChange() {
            this.id = nextChangeId();
        }

        public ProgrammedProblemFactChange(C change) {
            this.change = change;
        }

        public TaskAssigningSolution getSolutionAfterChange() {
            return solutionAfterChange;
        }

        public void setSolutionAfterChange(TaskAssigningSolution solutionAfterChange) {
            this.solutionAfterChange = solutionAfterChange;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public C getChange() {
            return change;
        }

        public void setChange(C change) {
            this.change = change;
        }
    }
}
