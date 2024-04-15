# JSON to XML Statistics Converter

This Java application provides a command-line interface for processing JSON files and generating XML statistics files based on specified attributes.

## Features

- Processes JSON files in a specified directory.
- Extracts specified attributes from JSON files.
- Generates XML statistics files for each attribute based on attribute value counts.
- Utilizes multi-threading for efficient processing.

## Usage

### Prerequisites

- Java 17 or higher
- Maven

### Installation

1. Clone this repository to your local machine:

   ```bash
   git clone https://github.com/vr242kj/JsonToXml.git
   ```
2. Go to the project directory
3. Build the project using Maven:
   ```bash
   mvn clean install
   ```
4. Run the application using Maven with the following command:
   ```
   mvn compile exec:java "-Dexec.args=<directory_path> <attribute_names>"
   ```
   Replace <directory_path> with the path to the directory containing your JSON files and <attribute_names> with a comma-separated list of attribute names without any spaces.  
   Example:
   ```
   mvn compile exec:java "-Dexec.args=/path/to/json/files attribute1,attribute2"
   ```
### Additional Information
These are simple examples of JSON schemas that the application operates with.
```
- [ { key: [value1, value2, value3] } ] array of values
- [ { key: [{},{},{}] } ]               array of objects
- [ { key: {} } ]                       object
- [ { key: value } ]                    value
```
The data in JSON files must be in an array (start with an array). For example:

```
[
  {
    "title": "1984",
    "author": "George Orwell",
    "year_published": 1949,
    "genre": "Dystopian, Political Fiction"
  },
  {
    "title": "Pride and Prejudice",
    "author": "Jane Austen",
    "year_published": 1813,
    "genre": "Romance, Satire"
  },
  {
    "title": "Romeo and Juliet",
    "author": "William Shakespeare",
    "year_published": 1597,
    "genre": "Romance, Tragedy"
  }
]
```
The result of the program is an XML file with the name statistics_by_attribute.xml  
In this case, the attribute is genre
```
<statistics>
  <item>
   <value>Romance</value>
   <count>2</count>
  </item>
  <item>
   <value>Dystopian</value>
   <count>1</count>
  </item>
  <item>
   <value>Satire</value>
   <count>1</count>
  </item>
  <item>
   <value>Political Fiction</value>
   <count>1</count>
  </item>
  <item>
   <value>Tragedy</value>
   <count>1</count>
  </item>
</statistics>
```
### Performance
For testing, eight JSON files were created with a total size of 35 MB.  
**Processor** - Intel(R) Core(TM) i5-8265U CPU @ 1.60GHz, 1800MHz, 4 cores, 8 logic processors

Performance test results with different numbers of threads:  
Test 1
- Execution time with thread pool size 1: 69 ms
- Execution time with thread pool size 2: 22 ms
- Execution time with thread pool size 4: 17 ms
- Execution time with thread pool size 8: 14 ms  

Test 2

- Execution time with thread pool size 1: 58 ms
- Execution time with thread pool size 2: 21 ms
- Execution time with thread pool size 4: 20 ms
- Execution time with thread pool size 8: 27 ms

Test 3

- Execution time with thread pool size 1: 56 ms
- Execution time with thread pool size 2: 17 ms
- Execution time with thread pool size 4: 15 ms
- Execution time with thread pool size 8: 18 ms

Test 4

- Execution time with thread pool size 1: 61 ms
- Execution time with thread pool size 2: 18 ms
- Execution time with thread pool size 4: 17 ms
- Execution time with thread pool size 8: 17 ms

**Conclusions:** According to the results of the tests we can observe performance improvement when using 2 threads, weak improvement when using 4 threads, but increasing up to 8 threads may not always be advantageous. It may depend on the amount of test data or technical characteristics of the computer on which the tests were conducted.
