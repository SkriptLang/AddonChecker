# JavaUsageChecker

## Usage

Usage: `java -jar JavaUsageChecker.jar <options>`

Possible options:
- `f:<file path>`
- `d:<directory path>`
- `q:<query>`

Query syntax: `<prefix>:<key>=<value>;<key>=<value>;<key>=<value>` etc

Possible prefixes are `c(lass)`, `f(ield)` and `m(ethod)`

- For all prefixes, the `n(ame)` key refers to the name of the respective object (class, field or method)
- For fields and methods, `o(wner)` refers to the class that defined the member
- For fields and methods, `d(escriptor)` refers to the (method or field) descriptor of the member

All values can be prefixed by either `[c]`, `[e]` or `[w] (`[c]` being the default)
These are different methods of string comparisons:
- `[c]` checks if the value is contained in the string
- `[e]` checks if the value is exactly equal to the string
- `[w]` checks if the value is a word (separated by `.`) within the string, mostly useful for class names


Examples:
- `java -jar JavaUsageChecker.jar f:MyJavaProgram.jar q:m:n=get`
- looks for method usages of methods with names containing 'get' in the file MyJavaProgram.jar
* `java -jar JavaUsageChecker.jar d:\"java testing\" q:c:n=[w]Info`
* looks for class usages of classes with names containing the word 'Info' in the directory 'java testing'

## Building

JavaUsageChecker uses Maven, with the build command `mvn package`