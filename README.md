# How to run the service
`mvn spring-boot:run` - to start the spring-boot application

#### 1. Naive (NaiveTradeEnrichmentService) version. 
The brute-force solution provides the desired functionality and sets the benchmark for the advanced solutions. 

`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-naive`

#### 2. DFThreadLocal (ThreadLocalDateFormaterTradeEnrichmentService) version. 
An advanced version of Naive solution where we use dedicated DateFormat
instance per each thread to avoid synchronization overhead on the all threads shared DateFormatter class.

`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-df-thread-local`

This solution gives us **> 83%** performance improvement in comparison with the naive version.

#### 3. EfficientStructures (EfficientStructuresTradeEnrichmentService) 
Version with efficient data structure leveraging and optimized lines processing

`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-efficient-structures`

There are 2 improvements made to the naive solution (**overall > 290% of performance increasing**). 

 - Since date validity check is rather 'expensive' operation, we could cache its valid values in a tread-safe HashSet to avoid redundant date validations.
The number of unique dates is relatively small, so the HashSet will be very efficient and memory-safe.
See EfficientStructuresTradeEnrichmentService#processLine.isValidDate for impl details.
 - Slightly improved logic of input lines parsing. Thus, we could avoid redundant string split operations
which performance is always O(n) and cut out only the necessary part of the line.
See EfficientStructuresTradeEnrichmentService#processLine for impl details.

#### 4. ReadWriteThreadsSplit (ThreadsSplitTradeEnrichmentService) is the fastest solution and comprises improvement techniques from both ReadWriteSplit, EfficientStructures solutions
`curl POST --form file="@./trade.csv" -X POST http://localhost:8080/api/v1/enrich-threads-split`

The idea was to split the reading and writing operations into separate threads to make them work in parallel.

- Consumer thread reads the input file, processes the lines, and puts them into the thread-safe queue.
- The producer thread reads the lines from the queue and writes them in the output file.

This solution gave us a tiny improvement in comparison with the EfficientStructures solution
and ~**300%** performance improvement compared to the naive version.

### Statistics summary per each solution: 

There is a Benchmark application in test sources that could be used to measure the performance of each solution.
Benchmark application runs medium-trade-file.csv 100k records file 20 times in a separate thread and measures the execution time in milliseconds.
Below there is a summary of the results per each solution:

| Solution                    | Min (ms) | Max (ms) | Avg (ms) | Improvement with Naive% |
|-----------------------------|----------|----------|----------|-------------------------|
| Naive                       | 984     | 1109     | 1058     | 0                       |
| DF Thread Local             | 507      | 615      | 577      | +83%                    |
| Efficient structures        | 239      | 298      | 271      | +290%                   |
| Read / Writes threads split | 194      | 317      | 264      | **+300%**               |


# Tests running and coverage

### How to build jar and run tests
`mvn clean install`

### Use-case coverage
4 tests are ensuring the reliable work of each solution:
- basic test asserting single-thread mode execution
- test asserting input with invalid lines
- thread-safety test asserting correct call handling in multi-thread mode
- empty file test asserting correct handling of empty input file
