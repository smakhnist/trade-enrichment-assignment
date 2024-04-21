# How to run the service
`mvn spring-boot:run` - to start the spring-boot application

#### 1. Naive (NaiveTradeEnrichmentService) version. The brute-force solution provides the desired functionality and sets the benchmark for the advanced solutions. 

`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-naive`

#### 2. ReadWriteSplit (ReadWriteTreadsSplitTradeEnrichmentService) is an advanced version of Naive solution where we split read and write operations to separate threads
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-read-write-split`

This solution gives us **> 100%** performance improvement in comparison with the naive version.
 - Consumer thread reads the input file, process the lines and put them into the thread-safe queue.
 - Producer thread reads the lines from the queue and writes them to the output file.

#### 3. EfficientStructures (EfficientStructuresTradeEnrichmentService) is a version with efficient data structure leveraging and optimized lines processing
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-efficient-structures`

There are 2 improvements made into naive solution (**overall > 300% of performance increasing**). 

 - Since date validity check is rather 'expensive' operation, we could cache its valid values in a tread-safe HashSet to avoid redundant date validations.
The number of unique dates is relatively small, so the HashSet will be very efficient and memory-safe.
See EfficientStructuresTradeEnrichmentService#processLine.isValidDate for impl details.
 - Slightly improved logic of input lines parsing. Thus, we could avoid redundant string split operations
which performance is always O(n) and cut out only the necessary part of the line.
See EfficientStructuresTradeEnrichmentService#processLine for impl details.

#### 4. Quick (QuickTradeEnrichmentService) is the fastest solution and comprises all the above solutions optimizations (ReadWriteSplit, EfficientStructures)
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-quick`

This solution gives us ~**450%** performance improvement compared to the naive version.

### Statistics summary per each solution: 

There is a Benchmark application in test sources that could be used to measure the performance of each solution.
Benchmark application runs medium-trade-file.csv 100k records file 20 times in a separate thread and measures the execution time in milliseconds.
Below there is a summary of the results per each solution:

| Solution                    | Min (ms) | Max (ms) | Avg (ms) | Improvement with Naive% |
|-----------------------------|----------|----------|----------|-------------------------|
| Naive                       | 2007     | 2182     | 2068     | 0                       |
| Read / Writes threads split | 528      | 1142     | 864      | +139%                   |
| Efficient structures        | 346      | 476      | 428      | +383%                   |
| Quick (all in one)          | 218      | 480      | 378      | **+447%**               |


# Test Coverage
3 tests are ensuring the reliable work of each solution:
- basic test asserting single-thread mode execution
- test asserting input with invalid lines
- thread-safety test asserting correct call handling in multi-thread mode

### How to build jar and run tests
`mvn clean install`