# Requirements
### Implementation
The candidate should implement a Java service, which will:
- Expose an API to enrich trade data (example: trade.csv) with product names from the static data file (product.csv)
- Translate the `product_id` into `product_name`.
- Perform data validation:
  - Ensure that the `date` is a _valid_ date in `yyyyMMdd` format, otherwise discard the row and log an error.
  - If the product name is not available, the service should log the missing mapping and set the product Name as: `Missing Product Name`.
- Be able to handle:
  - very large sets of trades (millions).
  - a large set of products (10k to 100k).
- Return the enriched trade data.

### Documentation
The candidate should modify the [README](./README.md) file to explain:
- How to run the service.
- How to use the API.
- Any limitations of the code.
- Any discussion/comment on the design.
- Any ideas for improvement if there were more time available.

### Assessment
⚠️ Please note that we are looking for:
- **_Clean design_**.
- **_Readable code_**.
- **_Good test coverage_**.
- **_Thread-safety_**.
- **_Efficiency_**.
- **_Clear documentation_**.

# Technical constraints
- The solution should be built on top of this skeleton project, which uses:
  - Spring boot 3.2.1
  - Java 17
  - Maven
    - 3rd party libraries can be used.
- ⚠️ Your solution should NOT require any external dependencies to run, e.g., a cache.

# Sample Data
## Sample static data file
### product.csv
```csv
product_id,product_name
1,Treasury Bills Domestic
2,Corporate Bonds Domestic
3,REPO Domestic
4,Interest rate swaps International
5,OTC Index Option
6,Currency Options
7,Reverse Repos International
8,REPO International
9,766A_CORP BD
10,766B_CORP BD
```
## Sample trade data file
### trade.csv
```csv
date,product_id,currency,price
20160101,1,EUR,10.0
20160101,2,EUR,20.1
20160101,3,EUR,30.34
20160101,11,EUR,35.34
```

# Sample Output
## Sample HTTP Request
This sample command may be modified depending on the implementation, but must be documented.
```curl
curl --data @src/test/resources/trade.csv --header 'Content-Type: text/csv' http://localhost:8080/api/v1/enrich
```
## Sample HTTP Response
```csv
date,product_name,currency,price
20160101,Treasury Bills Domestic,EUR,10
20160101,Corporate Bonds Domestic,EUR,20.1
20160101,REPO Domestic,EUR,30.34
20160101,Missing Product Name,EUR,35.34
```

# How to share the project
The candidate should upload the solution to a public GitHub repository from which we can download to review.