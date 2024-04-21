# How to run the service

#### 1. Naive (NaiveTradeEnrichmentService) version. Just make logic work

`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-naive`

#### 2. ReadWriteSplit (ReadWriteTreadsSplitTradeEnrichmentService) advanced version where we split read and write operations to separate threads
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-read-write-split`

This solution gives us **> 100%** performance improvement in comparison to the naive version.
 - Consumer thread reads the input file, process the lines and put them into the queue.
 - Producer thread reads the lines from the queue and writes them to the output file.

#### 3. EfficientStructures (EfficientStructuresTradeEnrichmentService) advanced with efficient structures and optimized lines processing
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-efficient-structures`

There are 2 significant improvements made into naive solution (**overall > 300% of performance increasing**). 

 - Since date validity check is pretty complex operation we could cache its success values in HashSet to avoid redundant date validations.
The number of unique dates is relatively small, so the HashSet will be very efficient.
See EfficientStructuresTradeEnrichmentService#processLine.isValidDate for impl details.
 - Slightly improved logic input lines parsing. Thus, we could avoid redundant string splitting operations
which performance is always O(n) and cut out only the necessary part of the line. It gives us a slight performance improvement.
See EfficientStructuresTradeEnrichmentService#processLine for impl details.

#### 4. Quick (QuickTradeEnrichmentService) is the fastest solution and comprises all the above solutions optimizations (ReadWriteSplit, EfficientStructures)
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-quick`

This solution gives us **~ 450%** performance improvement in comparison to the naive version.

### Statistics summary per each solution: 

There is a Benchmark application in test sources that could be used to measure the performance of each solution.
Benchmark application runs 20 times each solution in a separate thread and measures the time of execution.
Below there is a summary of the results per each solution:

| Solution                    | Min       | Max | Avg | Improvement with Naive% |
|-----------------------------|-----------|-----|-----|-------------------------|
| Naive                       | 2007      |    2182 |  2068   | 0                       |
| Read / Writes threads split | 528       |  1142   |   864  | +139%                   |
| Efficient structures        |   346        |  476   |  428   | +383%                   |
| Quick (all in one)          | 218 | 480 |   378  | **+447%**               |


# Test Coverage
There are 3 tests ensuring reliable work of each solution:
- basic test asserting single-thread mode execution
- test asserting input with invalid lines
- thread-safety test asserting calls correct handling in multi-thread mode

### How to build jar and run tests
`mvn clean install`