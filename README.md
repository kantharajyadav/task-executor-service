Task Executor Service.

This repository contains an implementation of a task executor service adhering to the following specifications.

The entry point for the service is Main.java. Details of the TaskExecutor interface, along with its dependencies, are defined within the codebase.

Key Features and Behaviors


	1.	Concurrent Task Submission:
	•	Tasks can be submitted concurrently.
	•	Submission is non-blocking for the submitter.
	2.	Asynchronous and Concurrent Execution:
	•	Tasks are executed asynchronously and concurrently.
	•	The maximum concurrency level can be restricted if needed.
	3.	Result Retrieval:
	•	Upon task completion, results can be retrieved from the Future object provided during submission.
	4.	Task Order Preservation:
	•	Tasks are executed in the order they are submitted.
	•	Results are made available as soon as the respective task completes.
	5.	Task Grouping:
	•	Tasks within the same TaskGroup are not executed concurrently, ensuring group-level exclusivity.
